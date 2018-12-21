package com.cqkct.FunKidII.Ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.cqkct.FunKidII.R;

public class CornerImageView extends AppCompatImageView {
    public CornerImageView(Context context) {
        super(context);
        init(context, null);
    }

    public CornerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CornerImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private Path path = new Path();
    private RectF rect = new RectF();
    private float[] radii = new float[]{0, 0, 0, 0, 0, 0, 0, 0};

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CornerImageView, 0, 0);
            try {
                setRadius(a.getDimension(R.styleable.CornerImageView_radius, 0));
                setRadius(
                        a.getDimension(R.styleable.CornerImageView_topLeftRadius, 0),
                        a.getDimension(R.styleable.CornerImageView_topRightRadius, 0),
                        a.getDimension(R.styleable.CornerImageView_bottomLeftRadius, 0),
                        a.getDimension(R.styleable.CornerImageView_bottomRightRadius, 0)
                );
            } finally {
                a.recycle();
            }
        }
    }

    public void setRadius(float radius) {
        setRadius(radius, radius, radius, radius);
    }

    public void setRadius(float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius) {
        radii[0] = topLeftRadius;
        radii[1] = topLeftRadius;
        radii[2] = topRightRadius;
        radii[3] = topRightRadius;
        radii[4] = bottomLeftRadius;
        radii[5] = bottomLeftRadius;
        radii[6] = bottomRightRadius;
        radii[7] = bottomRightRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        path.reset();
        rect.left = 0;
        rect.top = 0;
        rect.right = getWidth();
        rect.bottom = getHeight();
        path.addRoundRect(rect, radii, Path.Direction.CW);
        canvas.clipPath(path);
        super.onDraw(canvas);
    }
}
