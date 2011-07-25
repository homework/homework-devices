package uk.ac.nott.mrl.homework.client.model;

public interface ItemListener
{
	public void itemAdded(final Item item);

	public void itemRemoved(final Item item);

	public void itemUpdated(final Item item);

	public void itemUpdateFinished();
}
