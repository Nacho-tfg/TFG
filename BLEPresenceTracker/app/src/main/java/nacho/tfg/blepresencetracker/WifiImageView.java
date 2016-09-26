package nacho.tfg.blepresencetracker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Nacho on 08/06/2016.
 */
public class WifiImageView extends ImageView {
    private static final int[] STATE_LOCKED = {R.attr.state_locked};
    private boolean mWifiLocked;

    public WifiImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mWifiLocked) {
            mergeDrawableStates(drawableState, STATE_LOCKED);
        }
        return drawableState;
    }

    public void setStateLocked(boolean locked) {
        mWifiLocked = locked;
        refreshDrawableState();
    }
}
