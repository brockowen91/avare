/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ds.avare.MultiTouchController.MultiTouchObjectCanvas;
import com.ds.avare.MultiTouchController.PointInfo;
import com.ds.avare.MultiTouchController.PositionAndScale;

/**
 * 
 * @author zkhan
 *
 */
public class PlatesView extends View implements MultiTouchObjectCanvas<Object>, OnTouchListener {
	

    private Scale                        mScale;
    private Pan                          mPan;
	private Paint                        mPaint;
    private MultiTouchController<Object> mMultiTouchC;
    private PointInfo                    mCurrTouchPoint;
    private GestureDetector              mGestureDetector;
    private BitmapHolder                 mBitmap;
    private PixelCoordinates             mPixels;
    private boolean                     mDrag;
    
    /**
     * 
     * @param context
     */
	public PlatesView(Context context) {
		super(context);
		mPaint = new Paint();
		mPan = new Pan();
		mDrag = false;
		mPixels = new PixelCoordinates(0, 0);
		
		mScale = new Scale();
        setOnTouchListener(this);
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        mGestureDetector = new GestureDetector(context, new GestureListener());
        this.setBackgroundColor(Color.BLACK);
	}

    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent e) {
        if(e.getAction() == MotionEvent.ACTION_UP) {
        }
        mGestureDetector.onTouchEvent(e);
        return mMultiTouchC.onTouchEvent(e);
    }

    /**
     * @param name
     */
    public void setBitmap(BitmapHolder holder) {
        mBitmap = holder;
    }

    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#getDraggableObjectAtPoint(com.ds.avare.MultiTouchController.PointInfo)
     */
    public Object getDraggableObjectAtPoint(PointInfo pt) {
        return mBitmap;
    }

    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#getPositionAndScale(java.lang.Object, com.ds.avare.MultiTouchController.PositionAndScale)
     */
    public void getPositionAndScale(Object obj, PositionAndScale objPosAndScaleOut) {
        objPosAndScaleOut.set(mPan.getMoveX(), mPan.getMoveY(), true,
                mScale.getScaleFactor(), false, 0, 0, false, 0);
    }

    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#selectObject(java.lang.Object, com.ds.avare.MultiTouchController.PointInfo)
     */
    public void selectObject(Object obj, PointInfo touchPoint) {
        touchPointChanged(touchPoint);
    }

    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#setPositionAndScale(java.lang.Object, com.ds.avare.MultiTouchController.PositionAndScale, com.ds.avare.MultiTouchController.PointInfo)
     */
    public boolean setPositionAndScale(Object obj,PositionAndScale newObjPosAndScale, PointInfo touchPoint) {
        touchPointChanged(touchPoint);
        if(false == mCurrTouchPoint.isMultiTouch()) {
            /*
             * Multi-touch is zoom, single touch is pan
             */
            mPan.setMove(newObjPosAndScale.getXOff(), newObjPosAndScale.getYOff());
        }
        else {
            /*
             * Clamp scaling.
             */
            mScale.setScaleFactor(newObjPosAndScale.getScale());
        }
        invalidate();
        return true;
    }

    /**
     * Get points on the chart
     */
    public PixelCoordinates getPoints() {
    	return mPixels;
    }
    
    /**
     * Cancel point aquisition state machine and bring to init state. 
     */
    public void cancelState() {
    	mDrag = false;
    	postInvalidate();
    }

    /**
     * Run point acquire state machine to get two points on chart 
     */
    public void runState() {
    	if(mDrag == false) {
    		if(null != mBitmap) {
        		mPixels = new PixelCoordinates(mBitmap.getWidth(), mBitmap.getHeight());
    		}
    		mPixels.setX0(mPan.getMoveX());
    		mPixels.setY0(mPan.getMoveY());
    		mPixels.addPoint();
	    	mDrag = true;
    	}
    	else {
    		mPixels.addPoint();
    		mPixels.setX1(mPan.getMoveX());
    		mPixels.setY1(mPan.getMoveY());
    		mDrag = false;
    	}
    	postInvalidate();
    }

    /**
     * @param touchPoint
     */
    private void touchPointChanged(PointInfo touchPoint) {
        mCurrTouchPoint.set(touchPoint);
        invalidate();
    }

    /* (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    public void onDraw(Canvas canvas) {
    	if(mBitmap == null) {
    		return;
    	}
    	if(mBitmap.getBitmap() == null) {
    		return;
    	}
    	/*
    	 * Plate
    	 */
    	mBitmap.getTransform().setScale(mScale.getScaleFactor(), mScale.getScaleFactor());
    	mBitmap.getTransform().postTranslate(
    			mPan.getMoveX() * mScale.getScaleFactor() + getWidth() / 2 - mBitmap.getWidth() / 2 * mScale.getScaleFactor(),
    			mPan.getMoveY() * mScale.getScaleFactor() + getHeight() / 2 - mBitmap.getHeight() / 2 * mScale.getScaleFactor());
    	canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
    	mPaint.setColor(Color.RED);
    	mPaint.setStrokeWidth(4);
    	/*
    	 * Drag line for points
    	 */
    	if(mDrag) {
        	canvas.drawLine(
        			getWidth() / 2,
        			getHeight() / 2,
        			getWidth() / 2 + mPan.getMoveX() * mScale.getScaleFactor() - (float)mPixels.getX0() * mScale.getScaleFactor(),
        			getHeight() / 2 + mPan.getMoveY() * mScale.getScaleFactor() - (float)mPixels.getY0() * mScale.getScaleCorrected(),
        			mPaint);    		
    	}
    	/*
    	 * Cross hair
    	 */
    	canvas.drawLine(getWidth() / 2, getHeight() / 2 - 16, getWidth() / 2, getHeight() / 2 + 16, mPaint);
    	canvas.drawLine(getWidth() / 2 - 16, getHeight() / 2, getWidth() / 2 + 16, getHeight() / 2, mPaint);
    }
    
    /**
     * @author zkhan
     *
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            /*
             * On double tap, move to center
             */
            mPan = new Pan();
            return true;
        }

        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(MotionEvent e) {
        	
        }
    }
}