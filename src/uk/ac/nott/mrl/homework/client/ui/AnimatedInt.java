package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.Updatable;

public abstract class AnimatedInt implements Updatable
{
	public enum Style
	{
		constant, decellerating
	}

	private int current;
	private int target;
	private final int delta;

	private final int min;
	private final int max;

	public AnimatedInt(int value, int delta, int min, int max)
	{
		this.target = value;
		this.current = value;
		this.delta = delta;
		this.min = min;
		this.max = max;
		update(current);
	}

	public void setValue(final int value)
	{
		int newTarget = value;
		if (value > max)
		{
			newTarget = max;
		}
		else if (value < min)
		{
			newTarget = min;
		}

		if (target != newTarget)
		{
			target = newTarget;
			DevicesClient.updates.add(this);
		}
	}

	public int getValue()
	{
		return current;
	}

	public void setValueImmeadiately(final int value)
	{
		this.target = value;
		this.current = value;
		update(value);
	}

	public void setValueImmeadiately()
	{
		current = target;
		update(target);
	}

	public boolean update()
	{
		int change = Math.max(Math.min(Math.round(Math.abs(current - target) / 5), delta), 1);
		if (target > current)
		{
			if (target - current < change)
			{
				current = target;
			}
			else
			{
				current += change;
			}
			update(current);
		}
		else if (target < current)
		{
			if (current - target < change)
			{
				current = target;
			}
			else
			{
				current -= change;
			}
			update(current);
		}
		return current == target;
	}

	protected abstract void update(final int value);
}