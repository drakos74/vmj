package fine.vmj.fx.workspace.util;

import fine.vmj.util.Properties;

public class WorkspaceProperties implements Properties {

	public static final RangeProperty<Double> SCREEN_BRIGHTNESS = new RangeProperty<Double>("vmj.screen.brightness","",0.0,2.0,0.01,1.0);
	public static final RangeProperty<Integer> SCREEN_CONTRAST = new RangeProperty<Integer>("vmj.screen.contrast","",-100,100,10,0);
	public static final RangeProperty<Integer> SCREEN_GAMMA = new RangeProperty<Integer>("vmj.screen.gamma","",-10,10,1,0);

	public static final RangeProperty<Integer> SCREEN_HUE_START = new RangeProperty<Integer>("vmj.screen.hue.start","",0,180,5,20);
	public static final RangeProperty<Integer> SCREEN_HUE_STOP = new RangeProperty<Integer>("vmj.screen.hue.stop","",0,180,5,50);
	public static final RangeProperty<Integer> SCREEN_SATURATION_START = new RangeProperty<Integer>("vmj.screen.saturation.start","",0,255,5,60);
	public static final RangeProperty<Integer> SCREEN_SATURATION_STOP = new RangeProperty<Integer>("vmj.screen.saturation.stop","",0,255,5,200);
	public static final RangeProperty<Integer> SCREEN_VALUE_START = new RangeProperty<Integer>("vmj.screen.value.start","",0,255,5,50);
	public static final RangeProperty<Integer> SCREEN_VALUE_STOP = new RangeProperty<Integer>("vmj.screen.value.stop","",0,255,5,255);

	
}
