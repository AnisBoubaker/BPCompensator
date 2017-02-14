import java.util.logging.Logger;

import org.eclipse.emf.ecore.util.EcoreUtil;

import rea.ReaFactory;
import rea.Resource;
import rea.Right;
import rea.ValueModel;
import util.MyLogger;
import visitors.CompensationEngineVisitor;
import rea.visitors.GraphVizVisitor;
import visitors.SagaVisitor;


public class BPCompensator {

	private final static Logger LOGGER = Logger.getLogger(BPCompensator.class .getName());
	
	public static final void main(String[] args) {
		MyLogger.setup();
		
		REAModel amazon = new REAModel("AmazonShipDeliver.rea");
		ValueModel vm = amazon.getValueModel();
		
		Resource moneySettlement = ReaFactory.eINSTANCE.createResource();
		moneySettlement.setName("Settlement Resource");
		Right ownershipRight = ReaFactory.eINSTANCE.createRight();
		ownershipRight.setName("Ownership");
		moneySettlement.getRights().add(ownershipRight);
		vm.getResources().add(moneySettlement);
		
		GraphVizVisitor gv = new GraphVizVisitor();
		
		//Remove previously computed compensation process.
		if(vm.isSetCompensationProcess()) EcoreUtil.delete(vm.getCompensationProcess(), true);
		
		vm.getProcessDefinition().accept(gv);
		gv.save("AmazonShipDeliver");
		
		SagaVisitor saga = new SagaVisitor();
		vm.getProcessDefinition().accept(saga);
		vm.setCompensationProcess(saga.getCompensationBusinessProcess());
		amazon.save();
		
		vm.getCompensationProcess().accept(gv);
		gv.save("AmazonShipDeliver_saga");
		
		CompensationEngineVisitor compensator = new CompensationEngineVisitor(vm, moneySettlement);
		vm.getCompensationProcess().accept(compensator);
		amazon.save();
		
		vm.getCompensationProcess().accept(gv);
		gv.save("AmazonShipDeliver_comp");
		
		//File f = new File(".");
		//System.out.println(f.getAbsolutePath());
	}
	
}
