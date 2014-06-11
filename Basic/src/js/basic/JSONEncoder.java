package js.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static js.basic.Tools.*;

public class JSONEncoder {

	private StringBuilder sb = new StringBuilder();

	public String toString() {
		return sb.toString();
	}

	public void encode(IJSONEncoder jsonInstance) {
		jsonInstance.encode(this);
	}

	public void encode(Object value) {
		if (value instanceof Number)
			encode(((Number) value).doubleValue());
		else if (value instanceof Boolean)
			encode(((Boolean) value).booleanValue());
		else if (value == null)
			encodeNull();
		else if (value instanceof Map)
			encode((Map) value);
		else if (value instanceof List)
			encode((List) value);
		else if (value instanceof Set)
			encode((Set) value);
		else if (value instanceof Object[])
			encode((Object[]) value);
		else if (value instanceof int[])
			encode(cvtArray((int[]) value));
		else if (value instanceof double[])
			encode(cvtArray((double[]) value));
		else if (value instanceof String)
			encode((String) value);
		else
			throw new JSONException("unknown value type " + value + " : "
					+ value.getClass());
	}

	private static Object[] cvtArray(double[] value) {
		Object[] array = new Object[value.length];
		int i = 0;
		for (double x : value) {
			array[i++] = x;
		}
		return array;
	}

	private static Object[] cvtArray(int[] value) {
		Object[] array = new Object[value.length];
		int i = 0;
		for (int x : value) {
			array[i++] = x;
		}
		return array;
	}

	public void encode(Map map2) {
		// final boolean db = true;
		enterMap();

		Map<String, Object> map = (Map<String, Object>) map2;

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			encode(entry.getKey());
			Object value = entry.getValue();
			encode(value);
		}
		exitMap();
	}

	public void encode(List list) {
		encodeAsList(list.iterator());
	}

	private void encodeAsList(Iterator iter) {

		enterList();
		while (iter.hasNext()) {
			encode(iter.next());
		}
		exitList();
	}

	public void encode(Set set) {
		encodeAsList(set.iterator());
	}

	public void encode(Object[] array) {

		enterList();
		for (int i = 0; i < array.length; i++) {
			encode(array[i]);
		}
		exitList();
	}

	public void encode(double d) {
		prepareForNextValue();
		// final boolean db = true;
		long intValue = Math.round(d);
		if (d == intValue) {
			sb.append(intValue);
			if (db)
				pr(" encoding double " + d + " to int " + intValue);
		} else {
			if (db)
				pr(" encoding double " + d + " as double, since != intValue "
						+ intValue);
			sb.append(d);
		}
	}

	public void encode(String s) {
		prepareForNextValue();
		sb.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\n':
				sb.append("\\n");
				break;
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c >= ' ' && c < 0x7f)
					sb.append(c);
				else {
					sb.append(String.format("\\u%04x", (int) c));
				}
				break;
			}
		}
		sb.append('"');
	}

	public void encode(boolean b) {
		prepareForNextValue();
		sb.append(b ? "true" : "false");
	}

	public void encodeNull() {
		prepareForNextValue();
		sb.append("null");
	}

	public void clear() {
		sb.setLength(0);
	}

	public void enterList() {
		prepareForNextValue();
		pushState();
		collectionType = COLLECTION_LIST;
		collectionLength = 0;
		valueIsNext = false;
		sb.append('[');
	}

	public void exitList() {
		if (collectionType != COLLECTION_LIST)
			throw new IllegalStateException();
		sb.append(']');
		popState();
	}

	private void popState() {
		valueIsNext = (Boolean) pop(stateStack);
		collectionLength = (Integer) pop(stateStack);
		collectionType = (Integer) pop(stateStack);

	}

	private void pushState() {
		stateStack.add(collectionType);
		stateStack.add(collectionLength);
		stateStack.add(valueIsNext);
	}

	public void enterMap() {
		prepareForNextValue();
		pushState();
		collectionType = COLLECTION_MAP;
		collectionLength = 0;
		valueIsNext = false;
		sb.append('{');
	}

	public void exitMap() {
		if (collectionType != COLLECTION_MAP)
			throw new IllegalStateException();
		sb.append('}');
		popState();
	}

	private void prepareForNextValue() {
		switch (collectionType) {
		case COLLECTION_NONE:
			if (collectionLength != 0)
				throw new IllegalStateException(
						"multiple items while not within list or map");
			collectionLength++;
			break;
		case COLLECTION_LIST:
			if (collectionLength != 0)
				sb.append(',');
			collectionLength++;
			break;
		case COLLECTION_MAP:
			if (valueIsNext) {
				sb.append(':');
				valueIsNext = false;
			} else {
				valueIsNext = true;
				if (collectionLength != 0)
					sb.append(',');
				collectionLength++;
			}
			break;
		}
	}

	private static final int COLLECTION_NONE = 0, COLLECTION_LIST = 1,
			COLLECTION_MAP = 2;

	private int collectionType = COLLECTION_NONE;
	private int collectionLength;
	private boolean valueIsNext;
	private ArrayList stateStack = new ArrayList();

}