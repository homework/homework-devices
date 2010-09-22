package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.Updatable;

public abstract class AnimatedFloat implements Updatable
{
	private float current;
	private float target;
	private float delta;
	private float deltaLimit;
	private AnimationType type = AnimationType.decel;

	private final float min;
	private final float max;

	public AnimatedFloat(float value, float target, float delta, float min, float max)
	{
		this.current = value;
		this.min = min;
		this.max = max;
		setDelta(delta);
		setValue(target);
		update(current);
	}

	public AnimatedFloat(float value, float delta, float min, float max)
	{
		this.target = value;
		this.current = value;
		this.min = min;
		this.max = max;
		setDelta(delta);
		update(current);
	}

	private void setDelta(final float delta)
	{
		this.delta = delta;
		this.deltaLimit = delta / 50;
	}

	public void setValue(final float value)
	{
		float newTarget = value;
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

	public void setValueImmeadiately(final float value)
	{
		this.target = value;
		this.current = value;
		update(value);
	}

	public void setValue(final float value, final float delta, final AnimationType type)
	{
		this.type = type;
		setDelta(delta);
		setValue(value);
	}

	public abstract void update(final float value);

	public float getValue()
	{
		return current;
	}

	public boolean update()
	{
		float change = delta;
		if (type == AnimationType.decel)
		{
			change = Math.max(Math.min(Math.abs(current - target) / 5, delta), deltaLimit);
		}
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
}