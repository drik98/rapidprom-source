package org.rapidprom.ba;

import java.util.Collection;
import java.util.List;

public interface AixperantoContentWriter<T> {
	public String[] getFirstLine();
	public List<String[]> getContent(Collection<T> items);
	public String getFileName();
}
