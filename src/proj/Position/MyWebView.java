package proj.Position;

import android.R.integer;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.Toast;

public class MyWebView extends WebView {
	float pointX = 200;
	float pointY = 100;
	float pointR = 50;
	Context c;

	
	


	AlertDialog.Builder test;

	public MyWebView(Context context, AttributeSet attrs) {
		// TODO Auto-generated constructor stub

		super(context, attrs);
		c = context;
		this.setClickable(true);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int testp = Global.x; 
		float[] tx = { 50, testp };
		float[] ty = { 50, testp };

		if (event.getAction() == MotionEvent.ACTION_UP) {

			float tx1 = (getScrollX() + event.getX()) / getScale();
			float ty1 = (getScrollY() + event.getY()) / getScale();

			for (int i = 0; i <= 1; i++) {

				if ((tx1 - tx[i]) * (tx1 - tx[i]) + (ty1 - ty[i])
						* (ty1 - ty[i]) <= pointR * pointR) {
					Toast.makeText(c, "test", Toast.LENGTH_SHORT).show();

					test = new AlertDialog.Builder(c);
					test.setMessage("test");
					test.setTitle("test").setPositiveButton("OK",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// TODO Auto-generated method stub

								}
							});
					test.show();

				}

			}

		}
		return super.onTouchEvent(event);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int testp = Global.x; 
		float[] tx = { 50, testp };
		float[] ty = { 50, testp };
		// float x = pointX * getScale();
		// float y = pointY * getScale();
		//
		float r = pointR * getScale();
		// float r = pointR * 0.5f;

		Paint p = new Paint();

		for (int i = 0; i <= 1; i++) {
			float x = tx[i] * getScale();
			float y = ty[i] * getScale();
			canvas.drawCircle(x, y, r, p);

		}

	}

}
