package visitors;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.EList;

import rea.*;
import rea.impl.REAConceptVisitorImpl;
import rea.util.ReaProcessComparator;
import util.ReverseIterator;


public class SagaVisitor extends REAConceptVisitorImpl{
	private final static Logger LOGGER = Logger.getLogger(SagaVisitor.class .getName());
	
	private BusinessProcessDefinition compensationBP = null;
	private Stack<Object> visitorStack ;
	
	public SagaVisitor(){
		super();
		this.compensationBP = ReaFactory.eINSTANCE.createBusinessProcessDefinition();
		this.visitorStack = new Stack<Object>();
		LOGGER.setLevel(Level.INFO);
	}
	
	public BusinessProcessDefinition getCompensationBusinessProcess(){
		return this.compensationBP;
	}
	
	@Override
	public void visit(REAConcept concept){
		if(concept instanceof BusinessProcessDefinition) visitBusinessProcessDefinition((BusinessProcessDefinition)concept);
		else if (concept instanceof REAProcess) visitREAProcess((REAProcess)concept);
		else if (concept instanceof Event) visitEvent((Event)concept);
		else throw new UnsupportedOperationException();
	}
	
	public void visitBusinessProcessDefinition(BusinessProcessDefinition bp){
		
		bp.sortREAProcesses();
		
		//Run SAGA on the original process and call compensation on each REAProcess.
		for(ReverseIterator<REAProcess> it = new ReverseIterator<REAProcess>(bp.getREAProcesses()); it.hasNext() ; ){
			REAProcess p = it.next();
			LOGGER.fine("Compensating process with ID: ["+p.getID()+"]");
			
			p.accept(this);
			REAProcess cp = (REAProcess)visitorStack.pop();
			
			//Establish partial order of the new compensation process
			for(Iterator<REAProcess> it2 = p.getSuccessors().iterator(); it2.hasNext() ;){
				REAProcess successor = it2.next();
				cp.getPredecessors().add(successor.getCompensatedBy());
				LOGGER.fine("Compensation process ["+cp.getID()+"] has predecessor: ["+successor.getCompensatedBy().getID()+"]");
			}
			
			compensationBP.getREAProcesses().add(cp);
		}
	}
	
	
	public void visitREAProcess(REAProcess p){
		//Exchange cp = ReaFactory.eINSTANCE.createExchange();
		REAProcess cp;
		if(p instanceof Exchange) cp =  ReaFactory.eINSTANCE.createExchange();
		else cp =  ReaFactory.eINSTANCE.createConversion();
		
		for(Iterator<Event> it = p.getAllEvents().iterator(); it.hasNext() ; ){
			Event e = it.next();
			e.accept(this);
			Event cEvent = (Event)visitorStack.pop();
			
			if(e.isDecrementEvt()) cp.getIncrementEvt().add(cEvent);
			else cp.getDecrementEvt().add(cEvent);			
		}
		
		cp.setID(p.getID()+"^-1");
		cp.setCompensationFor(p);
		this.visitorStack.push(cp);
	}
	
	public void visitEvent(Event e){
		LOGGER.fine("Event: "+e.getName());
		
		Event cEvent = ReaFactory.eINSTANCE.createEvent();
		cEvent.setName(e.getName()+"^-1");
		
		cEvent.setResource(e.getResource());
		
		if(e.getREAProcess() instanceof Exchange) reverseTransfers(e, cEvent);
		else if(e.getREAProcess() instanceof Conversion) reverseAlterations(e, cEvent);
				
		cEvent.setProvider(e.getReceiver());
		cEvent.setReceiver(e.getProvider());
		
		visitorStack.push(cEvent);
	}
	
	private void reverseTransfers(Event oEvent, Event cEvent){
		EList<RightTransfer> transfers =  oEvent.getRightTransfers();
		for(Iterator<RightTransfer> it2 = transfers.iterator(); it2.hasNext(); ){
			RightTransfer transfer = it2.next();
			RightTransfer cTransfer = ReaFactory.eINSTANCE.createRightTransfer();
			cTransfer.setRight(transfer.getRight());
			cTransfer.setNewOwner(transfer.getInitialOwner());
			cEvent.getRightTransfers().add(cTransfer);
		}
	}
		
	private void reverseAlterations(Event oEvent, Event cEvent){
		EList<PropertyAlteration> alterations =  oEvent.getAlterations();
		for(Iterator<PropertyAlteration> it2 = alterations.iterator(); it2.hasNext(); ){
			PropertyAlteration alteration = it2.next();
			PropertyAlteration cAlteration = ReaFactory.eINSTANCE.createPropertyAlteration();
			cAlteration.setProperty(alteration.getProperty());
			cAlteration.setNewState(alteration.getInitialState());
			cEvent.getAlterations().add(cAlteration);
		}
	}
		
}
