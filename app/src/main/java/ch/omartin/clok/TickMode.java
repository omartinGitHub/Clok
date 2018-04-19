package ch.omartin.clok;

/**
 * Created by Olivier Martin on 16.03.2018.
 */
public enum TickMode
{
	MODE_12(12), MODE_24(24);

	private final int nbrHoursTicks;
	private final int nbrMinutesTicks = 60;

	private TickMode(int nbrHoursTicks)
	{
		this.nbrHoursTicks = nbrHoursTicks;
	}

	public int getNbrHoursTicks()
	{
		return nbrHoursTicks;
	}

	public int getNbrMinutesTicks()
	{
		return nbrMinutesTicks;
	}
}
