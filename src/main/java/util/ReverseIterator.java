package util;

import java.util.Iterator;
import org.eclipse.emf.common.util.EList;
/**
 * A custom iterator that traverses the business process graph in reverse order.  
 */
public class ReverseIterator <T> implements Iterator<T> {
	
	private EList<T> elements;
	private int currentIndex;
	

	public ReverseIterator(EList<T> l){
		this.elements = l;
		this.currentIndex = elements.size();
	}
	
	@Override
	public boolean hasNext() {
		return currentIndex>0;
	}

	@Override
	public T next() {
		currentIndex--;
		return elements.get(currentIndex);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
