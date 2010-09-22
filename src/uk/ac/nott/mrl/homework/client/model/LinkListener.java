package uk.ac.nott.mrl.homework.client.model;

public interface LinkListener
{
	public void linkAdded(final Link link, final int bandWidthMax);

	public void linkRemoved(final Link link);

	public void linkUpdated(final Link link, final int bandWidthMax);

	public void linkUpdateFinished();
}
