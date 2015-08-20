
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import rea.*;

public class REAModel {

	private ValueModel valueModel;
	private Resource resource;
	
	public REAModel(String modelResourcePath){
		load(modelResourcePath);
	}
	
	private ValueModel load(String filePath){
		// Initialize the model
		ReaPackage.eINSTANCE.eClass();
		
		// Register the XMI resource factory for the .rea extension
	    Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
	    Map<String, Object> m = reg.getExtensionToFactoryMap();
	    m.put("rea", new XMIResourceFactoryImpl());

	    // Obtain a new resource set
	    ResourceSet resSet = new ResourceSetImpl();

	    // Get the resource
	    this.resource = resSet.getResource(URI
	        .createURI("models/"+filePath), true);
	    // Get the first model element and cast it to the right type, in my
	    // example everything is hierarchical included in this first node
	    this.valueModel = (ValueModel) resource.getContents().get(0);
	    return this.valueModel;
	}
	
	public ValueModel getValueModel(){
		return this.valueModel;
	}
	
	public void save(){
		try {
	      resource.save(Collections.EMPTY_MAP);
	    } catch (IOException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	    }
	}
	
}
