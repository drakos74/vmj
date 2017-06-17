package fine.vmj.store.data;

public abstract class Frame implements IFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	final Rect rect;
	final int index;
	final int type;
	public Category cat;

	private String name;

	public Frame(Rect rect, int index, int type) {
		this.rect = rect;
		this.index = index;
		this.type = type;
		// make sure we have a category to start with ...
		cat = Category.values()[index];
	}

	public Rect getRect() {
		return rect;
	}

	public int getIndex() {
		return index;
	}

	public int getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return rect.toString() + "|" + cat + "[" + index + "]";
	}

}
