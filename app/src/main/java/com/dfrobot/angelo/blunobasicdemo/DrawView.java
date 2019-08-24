package com.dfrobot.angelo.blunobasicdemo;

import android.graphics.Canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.util.AttributeSet;

import com.jjoe64.graphview.series.DataPoint;

public class DrawView extends View
{
    private Paint paint = new Paint();
    private Line3D line = new Line3D();

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(15);
    }

    int arrowHeadLen = 40;
    @Override
    public void onDraw(Canvas canvas)
    {
        DataPoint start = line.getStartPos();
        DataPoint end = line.getEndPos();
        canvas.drawLine((int)(100 + start.getX()), (int)(100 + start.getY()),
                (int)(100 + end.getX()), (int)(100 + end.getY()), paint);
        double arrowHeadAngle = Math.atan2(start.getY(), start.getX());
        canvas.drawLine((int)(100 + start.getX()), (int)(100 + start.getY()),
                (int)(100+start.getX() + arrowHeadLen * Math.cos(arrowHeadAngle + 5*Math.PI / 6)),
                (int)(100+start.getY() + arrowHeadLen * Math.sin(arrowHeadAngle + 5*Math.PI / 6)), paint);
        canvas.drawLine((int)(100 + start.getX()), (int)(100 + start.getY()),
                (int)(100+start.getX() + arrowHeadLen * Math.cos(arrowHeadAngle - 5*Math.PI / 6)),
                (int)(100+start.getY() + arrowHeadLen * Math.sin(arrowHeadAngle - 5*Math.PI / 6)), paint);

        /*
        int arrowHeadLen = 40;
        canvas.drawLine((int)(100 + start.getX()), (int)(start.getY() + 100),
                (int)(100 + start.getX() + arrowHeadLen * Math.cos(line.angleX + 5*Math.PI/6) * Math.sin(line.angleY)),
                (int)(100 + start.getY() + arrowHeadLen * Math.sin(line.angleX) + 5*Math.PI/6), paint);
        /*
        canvas.drawLine((int)(100 + start.getX()), (int)(start.getY() + 100),
                (int)(100 + start.getX() + arrowHeadLen * Math.cos(line.angleX + 7*Math.PI/6)),
                (int)(100 + start.getY() + arrowHeadLen * Math.sin(line.angleX + 7*Math.PI/6)), paint);*/
    }

    public void setAngles(double angleX, double angleY)
    {
        line.angleX = angleX;
        line.angleY = angleY;
    }

    public void translate(double deltaXAngle, double deltaYAngle)
    {
        line.angleX += deltaXAngle;
        line.angleY += deltaYAngle;
    }

    public class Line3D
    {
        double length = 200;
        double angleX = 0,
                angleY = 0;

        private DataPoint getStartPos ()
        {
            return new DataPoint(length/2 * Math.cos(angleX) * Math.sin(angleY), length / 2 * Math.sin(angleX));
        }

        private DataPoint getEndPos ()
        {
            DataPoint start = getStartPos();
            return new DataPoint(-start.getX(), -start.getY());
        }
    }
}
