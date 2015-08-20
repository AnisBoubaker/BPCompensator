package visitors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;

import rea.Agent;
import rea.AgentAlias;
import rea.BusinessProcessDefinition;
import rea.Claim;
import rea.Conversion;
import rea.Event;
import rea.Exchange;
import rea.Property;
import rea.PropertyAlteration;
import rea.PropertyRecoverability;
import rea.REAConcept;
import rea.REAProcess;
import rea.ReaFactory;
import rea.Resource;
import rea.Right;
import rea.RightTransfer;
import rea.Valuable;
import rea.ValueModel;
import rea.impl.REAConceptVisitorImpl;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;


public class CompensationEngineVisitor extends REAConceptVisitorImpl {
	private final static Logger LOGGER = Logger.getLogger(CompensationEngineVisitor.class .getName());

	private ValueModel valueModel;
	private AgentAlias accountableAgent;
	private EList<REAProcess> processesToBeDeleted;
	private EList<Claim> claimsToSettle;
	private Valuable settlementResource;
	
	public CompensationEngineVisitor(ValueModel vm, Valuable settlementResource){
		super();
		this.valueModel = vm;
		this.processesToBeDeleted = new BasicEList<REAProcess>();
		this.claimsToSettle = new BasicEList<Claim>();
		this.settlementResource = settlementResource;
	}
	
	@Override
	public void visit(REAConcept concept){
		if(concept instanceof BusinessProcessDefinition) visitBusinessProcessDefinition((BusinessProcessDefinition)concept);
		else if (concept instanceof REAProcess) visitREAProcess((REAProcess)concept);
		else if (concept instanceof Event) visitEvent((Event)concept);
		else throw new UnsupportedOperationException();
	}
	
	private void visitBusinessProcessDefinition(BusinessProcessDefinition compensationBP){
		LOGGER.info("Compensation Engine started on business process: "+compensationBP.getName());
		for(Iterator<REAProcess> it = compensationBP.getREAProcesses().iterator() ; it.hasNext() ; ){
			REAProcess p = it.next();
			if(!this.processesToBeDeleted.contains(p)){
				p.accept(this);
			}
		}
		//Delete processes marked to be deleted
		for(REAProcess deleteProcess : this.processesToBeDeleted)
			EcoreUtil.delete(deleteProcess, true);
		
		settleClaims(compensationBP);
	}
	
	private void visitREAProcess(REAProcess p){
		LOGGER.info("Visiting REAProcess: ["+p.getID()+"]");
		for(Iterator<Event> it = p.getAllEvents().iterator() ; it.hasNext() ; ){
			Event e = it.next();
			e.accept(this);
		}
	}
	
	private void visitEvent(Event e){
		LOGGER.info("Visiting Event: ["+e.getName()+"]");
		if(e.getREAProcess() instanceof Conversion) compensatePropertyAlterations(e);
		else if(e.getREAProcess() instanceof Exchange) compensateRightExchanges(e);
				
	}
	
	private void compensateRightExchanges(Event e){
		
	}
	
	private void compensatePropertyAlterations(Event e){
		EList<PropertyAlteration> alterations = e.getAlterations();
		EList<Event> addToEvents;
		EList<PropertyAlteration> alterationMarkedForDeletetion = new BasicEList<PropertyAlteration>();
		
		addToEvents = e.getREAProcess().getIncrementEvt();
		
		
		//Prevent from creating multiple claims on the same resource if we have multiple property alterations in the same event. 
		Claim currentClaim = null;
		
		//Iterate through alterations and decide what to do for each property type/property compensability combination
		for(Iterator<PropertyAlteration> it = alterations.iterator(); it.hasNext() ; ){
			PropertyAlteration alteration = it.next();
			Property property = alteration.getProperty();
						
			LOGGER.finer("Compensating property: ["+property.getName()+"]");
			
			if(property.isPrimaryProperty() && alteration.getProperty().getRecoverable().equals(PropertyRecoverability.FULLY_RECOVERABLE)){
				LOGGER.finest("Property "+property.getName()+" is PRIMARY & FULLY_RECOVERABLE");
				//Nothing to do, revert the 
			} 
			else if(property.isPrimaryProperty() && alteration.getProperty().getRecoverable().equals(PropertyRecoverability.SEMI_RECOVERABLE)){
				LOGGER.finest("Property "+property.getName()+" is PRIMARY & SEMI_RECOVERABLE");
				if(currentClaim==null){
					Event claimEvent = createClaimEventOnResourceProperty(e.getREAProcess(), e, property);
					addToEvents.add(claimEvent);
					currentClaim = (Claim)(claimEvent.getResource());
				} else {
					currentClaim.getClaimForProperty().add(property);
				}
			}
			else if(property.isPrimaryProperty() && alteration.getProperty().getRecoverable().equals(PropertyRecoverability.NON_RECOVERABLE)){
				LOGGER.finest("Property "+property.getName()+" is PRIMARY & NON_RECOVERABLE");
				
				if(currentClaim==null){
					Event claimEvent = createClaimEventOnResourceProperty(e.getREAProcess(), e, null);
					addToEvents.add(claimEvent);
					currentClaim = (Claim)(claimEvent.getResource());
				} else {
					currentClaim.getClaimForProperty().add(property);
				}
				
				//Delete the previous original compensation event
				deleteSuccessorsBranch(e.getREAProcess(),property.getResource());
				EcoreUtil.delete(e, true);
				
				//No more properties should be considered since the whole resource is rendered not recoverable.
				break;
			} else if(!property.isPrimaryProperty() && alteration.getProperty().getRecoverable().equals(PropertyRecoverability.FULLY_RECOVERABLE)){
				LOGGER.finest("Property "+property.getName()+" is SECONDARY & FULLY_RECOVERABLE");
				//Nothing to do
			} else if(!property.isPrimaryProperty() && alteration.getProperty().getRecoverable().equals(PropertyRecoverability.SEMI_RECOVERABLE)){
				LOGGER.finest("Property "+property.getName()+" is SECONDARY & SEMI_RECOVERABLE");
				if(currentClaim==null){
					Event claimEvent = createClaimEventOnResourceProperty(e.getREAProcess(), e, property);
					addToEvents.add(claimEvent);
					currentClaim = (Claim)(claimEvent.getResource());
				} else {
					currentClaim.getClaimForProperty().add(property);
				}
			} else if(!property.isPrimaryProperty() && alteration.getProperty().getRecoverable().equals(PropertyRecoverability.NON_RECOVERABLE)){
				LOGGER.finest("Property "+property.getName()+" is SECONDARY & NON_RECOVERABLE");
				if(currentClaim==null){
					Event claimEvent = createClaimEventOnResourceProperty(e.getREAProcess(), e, property);
					addToEvents.add(claimEvent);
					currentClaim = (Claim)(claimEvent.getResource());
					//Remove the alteration
					alterationMarkedForDeletetion.add(alteration);
				} else {
					currentClaim.getClaimForProperty().add(property);
				}
			}
						
		}
		
		//Delete alterations marked for deletetion
		for(PropertyAlteration alteration : alterationMarkedForDeletetion)
			EcoreUtil.delete(alteration, true);
		
		if(currentClaim!=null) this.claimsToSettle.add(currentClaim);
	}
	
	private void deleteSuccessorsBranch(REAProcess p, Valuable r){
		this.processesToBeDeleted.addAll(p.getSuccessorsBranchForResource(r));
	}
	
	private Event createClaimEventOnResourceProperty(REAProcess process, Event event, Property property){
		//Create a claim on the loss due to the alteration
		Resource resource = (Resource)event.getResource();
		Claim claim = ReaFactory.eINSTANCE.createClaim();
		claim.setClaimForResource(resource);
		if(property!=null) claim.getClaimForProperty().add(property);
		claim.setName(resource.getName());
		this.valueModel.getResources().add(claim);
		
		Right ownershipRight = ReaFactory.eINSTANCE.createRight();
		ownershipRight.setName("Ownership");
		ownershipRight.setFirstOwner(event.getReceiver());
		claim.getRights().add(ownershipRight);
										
		Event claimEvent = ReaFactory.eINSTANCE.createEvent();
		claimEvent.setName("Issue Claim");
		claimEvent.setReceiver(event.getReceiver());
		claimEvent.setProvider(getAccountableAgent());
		claimEvent.setResource(claim);
				
		return claimEvent;
	}
	
	private Agent getAccountableAgent(){
		if(this.accountableAgent==null){
			AgentAlias a = this.valueModel.getAccountableAgent();
			if(a==null){
				this.accountableAgent = ReaFactory.eINSTANCE.createAgentAlias();
				this.accountableAgent.setName("Accountable Agent");
				this.accountableAgent.setAccountableAgent(true);
				this.valueModel.getAgents().add(this.accountableAgent);
			} else{
				this.accountableAgent = a;
			}
		}
		return this.accountableAgent; 
	}
	
	private void settleClaims(BusinessProcessDefinition compensationBP){
		LOGGER.fine("Settling claims");
		if(this.claimsToSettle.size()==0) return;
		HashMap<Agent, REAProcess> agentProcessMap = new HashMap<Agent, REAProcess>();
		
		for(Claim c : this.claimsToSettle){
			LOGGER.finer("Settling claim: "+c.getName());
			Agent claimOwner = getOwnershipRight(c).getFirstOwner();
			REAProcess settlementProcess = agentProcessMap.get(claimOwner);
			if(settlementProcess==null){
				settlementProcess = ReaFactory.eINSTANCE.createExchange();
				settlementProcess.setID("E-Settlement");
				compensationBP.getREAProcesses().add(settlementProcess);
				agentProcessMap.put(claimOwner, settlementProcess);
			}
			//Each claim is related to one event prior to settlement.
			//The REAProcess containing the event should be a predecessor of the Settlement Process.
			settlementProcess.getPredecessors().add(c.getEvents().get(0).getREAProcess());
			
			Event provideClaimEvent = ReaFactory.eINSTANCE.createEvent();
			provideClaimEvent.setResource(c);
			provideClaimEvent.setName("Provide Claim");
			provideClaimEvent.setReceiver(this.getAccountableAgent());
			provideClaimEvent.setProvider(getOwnershipRight(c).getFirstOwner());
			settlementProcess.getDecrementEvt().add(provideClaimEvent);
			
			RightTransfer transfer = ReaFactory.eINSTANCE.createRightTransfer();
			transfer.setRight(getOwnershipRight(c));
			transfer.setNewOwner(provideClaimEvent.getReceiver());
			provideClaimEvent.getRightTransfers().add(transfer);
		}
		for(Iterator<Map.Entry<Agent,REAProcess>> it = agentProcessMap.entrySet().iterator() ; it.hasNext() ; ) {
			Map.Entry<Agent,REAProcess> pairs = it.next();
			Agent receiverAgent = pairs.getKey();
			REAProcess settlementProcess = pairs.getValue();
			
			//Add the settlement event to each REAProcess of the HashMap 
			Event settlementEvent = ReaFactory.eINSTANCE.createEvent();
			settlementEvent.setName("Settle Claims");
			settlementEvent.setResource(this.settlementResource);
			settlementEvent.setReceiver(receiverAgent);
			settlementEvent.setProvider(this.getAccountableAgent());
			RightTransfer transfer = ReaFactory.eINSTANCE.createRightTransfer();
			transfer.setRight(getOwnershipRight(this.settlementResource));
			transfer.setNewOwner(settlementEvent.getReceiver());
			settlementEvent.getRightTransfers().add(transfer);
			settlementProcess.getIncrementEvt().add(settlementEvent);
		}
		
	}
	
	private Right getOwnershipRight(Valuable r){
		for(Right right : r.getRights()){
			if(right.getName().equals("Ownership")) 
				return right;
		}
		return null;
	}
}
