package com.dreamlive.hotimglibrary.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.dreamlive.hotimglibrary.R;
import com.dreamlive.hotimglibrary.entity.HotArea;
import com.dreamlive.hotimglibrary.utils.LogUtils;
import com.dreamlive.hotimglibrary.utils.XMLUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 不规则图片区域点击响应
 */
public class HotClickView extends View {

    private final static String TAG = HotClickView.class.getName();

    private final Context mContext;
    //热点区
    private Map<String, HotArea> mHotAreas;
    //检测的区域
    private Map<String, HotArea.CheckArea> mCheckAreas;
    //热点的Key值
    private Set<String> mHotKeys;
    //原图片
    private Bitmap mSourceBitmap = null;
    //用于保存Matrix的值,
    private float[] mValues;
    //最大与最小缩放值
    private float minScale, maxScale;
    //
    private Matrix mSaveMatrix, mMatrix;
    //记录点的位置
    private PointF mPointF, mMidPointF;
    //触摸前两指间的距离
    private float mBeforeDistance;
    //是否是多点触摸
    private boolean mIsTwoFinger;
    //视图宽度
    private static int VIEW_WIDTH = 0;
    //视图高度
    private static int VIEW_HEIGHT = 0;
    //原图宽度
    private static int BIT_WIDTH = 0;
    //原图高度
    private static int BIT_HEIGHT = 0;

    //按下时的时间,用于检测点击事件
    private long mDownTime;

    //Path中转RectF时的中间变量,可重复利用
    private final RectF mEmptyRectF = new RectF();


    //反弹时的线程
    protected boolean isAnimation;

    //是否能移动
    private boolean isCanMove = true;
    //是否能点击
    private boolean isCanClick = true;
    //是否边界检测
    protected boolean isNeedToCheckOutOfSide = true;
    //是否能缩放
    private boolean isCanScale = true;

    private MotionEvent lastClick = null;

    // 控件 内边距
    private float mPadding = 0;

    private short mFitXY = 0;

    //不进行适配
    public final static short FIT_NONE = 0;
    //X方向适配
    public final static short FIT_X = 1;
    //Y方向适配
    public final static short FIT_Y = 2;
    //XY方向适配，以最小作为标准
    public final static short FIT_XY = 3;

    private HotArea mRootArea;

    // 监听 回调事件
    private OnClickListener mClickListener;

    @SuppressLint("HandlerLeak")
    protected Handler mViewHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            Float[] distance = (Float[]) msg.obj;
            mMatrix.postTranslate(distance[0], distance[1]);
            invalidate();
        }

        ;
    };

    public HotClickView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public HotClickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    public HotClickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public void setCanScale(boolean canScale) {
        isCanScale = canScale;
    }

    public void setCanMove(boolean canMove) {
        isCanMove = canMove;
    }

    protected void init() {
        mCheckAreas = new HashMap<String, HotArea.CheckArea>();
        mHotAreas = new HashMap<String, HotArea>();
        mHotKeys = new LinkedHashSet<String>();
        mPointF = new PointF();
        mMidPointF = new PointF();
        mSaveMatrix = new Matrix();
        mMatrix = new Matrix();
        mValues = new float[9];
        maxScale = 4f;
        minScale = 1f;
        mPadding = getResources().getDimension(R.dimen.margin_large);
        //获取View的高与宽,并将图片放于中间位置
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
                VIEW_HEIGHT = getHeight();
                VIEW_WIDTH = getWidth();
                moveToCenter(mSourceBitmap);
                scaleToFit(mSourceBitmap);
            }
        });
    }

    public void reset() {
        mFitXY = 0;
        mHotAreas.clear();
        mCheckAreas.clear();
        mHotKeys.clear();
        mPointF.x = 0;
        mPointF.y = 0;
        mMidPointF.x = 0;
        mMidPointF.y = 0;
        mSaveMatrix.reset();
        minScale = 1.0f;
        maxScale = 4.0f;
        mMatrix.reset();
        for (int i = 0; i < 9; ++i) {
            mValues[i] = 0;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSourceBitmap != null) {
            canvas.drawBitmap(mSourceBitmap, mMatrix, null);
            drawPath(canvas);
//			drawRect(canvas);
        } else {
            LogUtils.d(TAG, "mSourceBitmap is null !");
        }
    }

    private void drawPath(Canvas canvas) {
        for (String key : mHotKeys) {
            float scale = getCurrentScale();
            Paint paint = new Paint();
//			paint.setColor(Color.BLUE);
            paint.setARGB(80, 68, 173, 161);
            paint.setStyle(Style.FILL);
            canvas.scale(scale, scale);
            canvas.translate((VIEW_WIDTH / scale - BIT_WIDTH) / 2, (VIEW_HEIGHT/scale-BIT_HEIGHT)
                    / 2);
            canvas.drawPath(mCheckAreas.get(key).getPath(), paint);
        }
    }

    private void drawRect(Canvas canvas) {
        Paint paint = new Paint();
        if (mEmptyRectF != null && !mEmptyRectF.isEmpty()) {
            paint.setColor(Color.GREEN);
            paint.setStyle(Style.FILL);
            canvas.drawRect(mEmptyRectF, paint);
        }
        if (lastClick != null) {
            float[] curMove = getCurrentMoveXY();
            float scale = getCurrentScale();
            paint.setColor(Color.RED);
            canvas.drawCircle((lastClick.getX() - curMove[0]) / scale, (lastClick.getY() - curMove[1]) / scale, 10, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean resStat = super.onTouchEvent(event);
        if (mSourceBitmap != null && !isAnimation) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mDownTime = System.currentTimeMillis();
                mPointF.set(event.getX(), event.getY());
            }
            if (event.getPointerCount() == 1) {
                if (isCanMove) {
                    moveEvent(event);
                    resStat = true;
                }
                if (isCanClick) {
                    clickEvent(event);
                    resStat = true;
                }
            } else if (event.getPointerCount() == 2) {
                if (isCanScale) {
                    scaleEvent(event);
                    resStat = true;
                }
            }
            if (isNeedToCheckOutOfSide) {
                outOfSideEvent(event);
                resStat = true;
            }
        }
        return resStat;
    }

    /**
     * 是否超出边界检测,进行回退到边界位置
     *
     * @param event
     */
    protected void outOfSideEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                upToCheckOutOfSide(event);
                break;
            default:
                break;
        }
    }

    /**
     * 点击事件检测
     *
     * @param event
     */
    protected void clickEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                upToCheckIsClick(event);
                break;
            default:
                break;
        }
    }

    /**
     * 移动事件检测
     *
     * @param event
     */
    protected void moveEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mSaveMatrix.set(mMatrix);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mIsTwoFinger) {
                    mMatrix.set(mSaveMatrix);
                    float moveX = event.getX() - mPointF.x;
                    float moveY = event.getY() - mPointF.y;
                    mMatrix.postTranslate(moveX, moveY);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsTwoFinger = false;
                break;
            default:
                break;
        }
    }

    /**
     * 边界反弹事件检测
     *
     * @param event
     */
    protected void upToCheckOutOfSide(MotionEvent event) {
        float scale = getCurrentScale();
        float[] moveXY = getCurrentMoveXY();
        float curBitWidth = scale * BIT_WIDTH;
        float curBitHeight = scale * BIT_HEIGHT;
        float[] dstXY = new float[2];
        boolean needMove = false;
        dstXY[0] = moveXY[0];
        dstXY[1] = moveXY[1];
        if (curBitHeight <= VIEW_HEIGHT) {
            needMove = true;
            dstXY[1] = (VIEW_HEIGHT - curBitHeight) / 2.0f;
        }

        if (curBitWidth <= VIEW_WIDTH) {
            needMove = true;
            dstXY[0] = (VIEW_WIDTH - curBitWidth) / 2.0f;
        }

        if (curBitHeight > VIEW_HEIGHT) {
            float distance = event.getY() - mPointF.y;
            if (distance > 0) {
                if (moveXY[1] > 0) {
                    dstXY[1] = 0;
                    needMove = true;
                }
            } else if (distance < 0) {
                float goalY = VIEW_HEIGHT - curBitHeight;
                if (moveXY[1] < goalY) {
                    dstXY[1] = goalY;
                    needMove = true;
                }
            }
        }

        if (curBitWidth > VIEW_WIDTH) {
            float distance = event.getX() - mPointF.x;
            if (distance > 0) {
                if (moveXY[0] > 0) {
                    dstXY[0] = 0;
                    needMove = true;
                }
            } else if (distance < 0) {
                float goalX = VIEW_WIDTH - curBitWidth;
                if (moveXY[0] < goalX) {
                    dstXY[0] = goalX;
                    needMove = true;
                }
            }
        }
        if (needMove) {
            mViewHandler.postDelayed(new MoveRunnable(moveXY[0], moveXY[1], dstXY[0], dstXY[1]), 0);
        }
    }

    /**
     * 反弹时的动画线程
     *
     * @author yq
     */
    protected class MoveRunnable implements Runnable {

        private final static int MOVE_STEEP = 20;

        private final float direct;

        private final boolean isMoveX;

        private float srcX, srcY;

        private final float dstX;

        private final float dstY;

        //一元一次方程
        private float a, b;

        public MoveRunnable(float srcX, float srcY, float dstX, float dstY) {
            this.srcX = srcX;
            this.srcY = srcY;
            this.dstX = dstX;
            this.dstY = dstY;
            //求解A，B
            if ((dstY - srcY) != 0 && (dstX - srcX) != 0) {
                a = (dstY - srcY) / (dstX - srcX);
                b = dstY - a * dstX;
            }
            //以长的作为出发点
            isMoveX = Math.abs(srcX - dstX) > Math.abs(srcY - dstY);
            direct = isMoveX ? ((dstX - srcX) > 0 ? 1.0f : -1.0f) : ((dstY - srcY) > 0 ? 1.0f : -1.0f);
            isAnimation = true;
        }

        @Override
        public void run() {
            float distanceX = 0;
            float distanceY = 0;
            boolean isEnd = false;
            if (isMoveX) {
                distanceX = direct * MOVE_STEEP;
                srcX += distanceX;
                if (direct > 0) {
                    if (srcX >= dstX) {
                        isEnd = true;
                        srcX -= distanceX;
                        distanceX = dstX - srcX;
                        srcX = dstX;
                    }
                } else {
                    if (srcX <= dstX) {
                        isEnd = true;
                        srcX -= distanceX;
                        distanceX = dstX - srcX;
                        srcX = dstX;
                    }
                }

                if (a == 0 && b == 0) {
                    distanceY = 0;
                } else {
                    float tempY = a * srcX + b;
                    distanceY = tempY - srcY;
                    srcY = tempY;
                }
            } else {
                distanceY = direct * MOVE_STEEP;
                srcY += distanceY;
                if (direct > 0) {
                    if (srcY >= dstY) {
                        isEnd = true;
                        srcY -= distanceY;
                        distanceY = dstY - srcY;
                        srcY = dstY;
                    }
                } else {
                    if (srcY <= dstY) {
                        isEnd = true;
                        srcY -= distanceY;
                        distanceY = dstY - srcY;
                        srcY = dstY;
                    }
                }

                if (a == 0 && b == 0) {
                    distanceX = 0;
                } else {
                    float tempX = (srcY - b) / a;
                    distanceX = tempX - srcX;
                    srcX = tempX;
                }
            }
            mViewHandler.obtainMessage(0, new Float[]{distanceX, distanceY}).sendToTarget();
            if (!isEnd) {
                mViewHandler.postDelayed(this, 10);
            } else {
                isAnimation = false;
                LogUtils.d(TAG, isAnimation + ", End!");
            }
        }
    }

    /**
     * 当前默认只取最前面一个
     *
     * @param
     */
    protected void upToCheckIsClick(MotionEvent event) {
        long curTime = System.currentTimeMillis() - mDownTime;
        if (curTime < 200) {
            checkAreas(event);
            if (!mHotKeys.isEmpty()) {
                HotArea area = null;
                for (String key : mHotKeys) {
                    area = mHotAreas.get(key);
                    mClickListener.OnClick(this, area);
                    break;
                }
            }
        }
    }

    /**
     * 缩放事件
     *
     * @param event
     */
    protected void scaleEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mIsTwoFinger = true;
                mBeforeDistance = spacing(event);
                if (mBeforeDistance > 10f) {
                    mSaveMatrix.set(mMatrix);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float afterDistance = spacing(event);
                if (afterDistance > 10f) {
                    float tempScale = afterDistance / mBeforeDistance;
                    mMatrix.set(mSaveMatrix);
                    float newScale = getCheckRangeScale(tempScale);
//				imageCenterLocation(event);
                    mMatrix.postScale(newScale, newScale, VIEW_WIDTH / 2, VIEW_HEIGHT / 2);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() == 0) {
                    upToCheckOutOfSide(event);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 两点触摸时的距离
     *
     * @param event
     * @return
     */
    @SuppressLint("FloatMath")
    protected float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 返回当前的ScaleX
     */
    public float getCurrentScale() {
        mMatrix.getValues(mValues);
        return Math.abs(mValues[0] == 0 ? mValues[1] : mValues[0]);
    }

    /**
     * 返回当前的移动位置
     *
     * @return
     */
    public float[] getCurrentMoveXY() {
        mMatrix.getValues(mValues);
        return new float[]{mValues[2], mValues[5]};
    }

    /**
     * 检测是否在当前的缩放范围里
     *
     * @param tempScale
     * @return
     */
    protected float getCheckRangeScale(float tempScale) {
        mMatrix.getValues(mValues);
        float value = Math.abs(mValues[0]) + Math.abs(mValues[1]);
        float newScale = value * tempScale;
        if (newScale < minScale)
            return minScale / value;
        else if (newScale > maxScale)
            return maxScale / value;
        return tempScale;
    }

    /**
     * 缩放的中心位置
     *
     * @param event
     */
    protected void imageCenterLocation(MotionEvent event) {
        mMidPointF.x = (event.getX(0) + event.getX(1)) / 2;
        mMidPointF.y = (event.getY(0) + event.getY(1)) / 2;
    }

    /**
     * 是否点击在热点区检测
     *
     * @param event
     */
    protected void checkAreas(MotionEvent event) {
        mHotKeys.clear();
        float[] curMove = getCurrentMoveXY();
        float scale = getCurrentScale();
        for (String key : mCheckAreas.keySet()) {
            if (mCheckAreas.get(key).isInArea(mEmptyRectF, (event.getX() - curMove[0]) / scale, (event.getY() - curMove[1]) / scale)) {
                mHotKeys.add(key);
                lastClick = event;
                break;
            }
        }
    }

    /**
     * @param bitmap
     */
    public void setImageBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap, FIT_NONE);
    }

    /**
     * 设置图片
     *
     * @param bitmap
     */
    public void setImageBitmap(Bitmap bitmap, short fitXY) {
        reset();
        mFitXY = fitXY;
        isCanClick = false;
        mSourceBitmap = bitmap;
        if (mSourceBitmap != null) {
            BIT_WIDTH = mSourceBitmap.getWidth();
            BIT_HEIGHT = mSourceBitmap.getHeight();
        }
        invalidate();
    }

    public void setImageBitmap(String hotFilePath, String hotImgPath) {
        setImageBitmap(hotFilePath, hotImgPath, FIT_NONE);
    }

    /**
     * 从资源文件中加载xml与picture
     *
     * @param hotFilePath:热点区域的位置定义文件(.xml)
     * @param hotImgPath:图片位置
     * @param fitXY                         [0, 1, 2, 3]
     *                                      0: no fit
     *                                      1: fitx
     *                                      2: fity
     *                                      3: fitxy get min scale X,Y
     */
    public void setImageBitmap(String hotFilePath, String hotImgPath, short fitXY) {
        reset();
        mFitXY = fitXY;
        mRootArea = XMLUtils.getInstance(mContext).readDoc(hotFilePath);
        mSourceBitmap = BitmapFactory.decodeFile(hotImgPath);
        resetFiles();
    }

    /**
     * 设置图片，从流中加载
     *
     * @param hotFileStream
     * @param hotImgStream  0: no fit
     *                      1: fitx
     *                      2: fity
     *                      3: fitxy get min scale X,Y
     */
    public void setImageBitmap(InputStream hotFileStream, InputStream hotImgStream) {
        setImageBitmap(hotFileStream, hotImgStream, FIT_NONE);
    }

    /**
     * 设置图片，从流中加载
     *
     * @param hotFileStream
     * @param hotImgStream
     * @param fitXY         [0, 1, 2, 3]
     *                      0: no fit
     *                      1: fitx
     *                      2: fity
     *                      3: fitxy get min scale X,Y
     */
    public void setImageBitmap(InputStream hotFileStream, InputStream hotImgStream, short fitXY) {
        reset();
        mFitXY = fitXY;
        mRootArea = XMLUtils.getInstance(mContext).readDoc(hotFileStream);
        mSourceBitmap = BitmapFactory.decodeStream(hotImgStream);
        resetFiles();
    }

    /**
     * 图片信息重置
     */
    protected void resetFiles() {
        try {
            if (mSourceBitmap != null) {
                BIT_HEIGHT = mSourceBitmap.getHeight();
                BIT_WIDTH = mSourceBitmap.getWidth();
            }
            moveToCenter(mSourceBitmap);
            scaleToFit(mSourceBitmap);
            addToKeys(mRootArea);
            invalidate();
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    /**
     * 移动到中间位置
     *
     * @param bitmap
     */
    protected void moveToCenter(Bitmap bitmap) {
        if (bitmap != null && VIEW_HEIGHT != 0 && VIEW_WIDTH != 0) {
            mMatrix.setTranslate((VIEW_WIDTH - BIT_WIDTH) / 2, (VIEW_HEIGHT - BIT_HEIGHT) / 2);
        }
    }

    /**
     * 缩放到合适大小
     *
     * @param bitmap
     */
    protected void scaleToFit(Bitmap bitmap) {
        if (bitmap != null && VIEW_HEIGHT != 0 && VIEW_WIDTH != 0) {
            float newScaleX = minScale;
            float newScaleY = minScale;
            if (mFitXY == 1 || mFitXY == 3) {
//				if(BIT_WIDTH > VIEW_WIDTH) {
                newScaleX = (VIEW_WIDTH * 1.0f) / BIT_WIDTH;
                LogUtils.d(TAG, "newScaleX:" + newScaleX);
//				}
            }
            if (mFitXY == 2 || mFitXY == 3) {
//				if(BIT_HEIGHT > VIEW_HEIGHT) {
                newScaleY = ((VIEW_HEIGHT - mPadding * 2) * 1.0f) / BIT_HEIGHT;
                LogUtils.d(TAG, "newScaleY:" + newScaleY);
//				}
            }
            minScale = Math.min(newScaleX, newScaleY);
            mMatrix.postScale(minScale, minScale, VIEW_WIDTH / 2, VIEW_HEIGHT / 2);
        }
    }

    protected void scaleToFit1(Bitmap bitmap) {
        if (bitmap != null && VIEW_HEIGHT != 0 && VIEW_WIDTH != 0) {
            float newScaleX = minScale;
            float newScaleY = minScale;
            if (mFitXY == 1 || mFitXY == 3) {
                newScaleX = (VIEW_WIDTH * 1.0f) / BIT_WIDTH;
                Log.d(TAG, "newScaleX:" + newScaleX);
            }
            if (mFitXY == 2 || mFitXY == 3) {
                newScaleY = ((VIEW_HEIGHT - mPadding * 2) * 1.0f) / BIT_HEIGHT;
                Log.d(TAG, "newScaleY:" + newScaleY);
            }
            minScale = Math.min(newScaleX, newScaleY);
            mMatrix.postScale(minScale, minScale, VIEW_WIDTH / 2,
                    VIEW_HEIGHT / 2);
        }
    }

    public HotArea getRootArea() {
        return mRootArea;
    }

    /**
     * 添加到热点集合中
     *
     * @param area
     */
    protected void addToKeys(HotArea area) {
        if (area != null) {
            String areaCode = area.getAreaId();
            HotArea.CheckArea checkArea = area.getCheckArea();
            mHotAreas.put(areaCode, area);
            if (checkArea != null) {
                mCheckAreas.put(areaCode, checkArea);
            }
            for (HotArea hot : area.getAreas()) {
                addToKeys(hot);
            }
        }
    }

    /**
     * 设置点击 事件
     */
    public void setOnClickListener(OnClickListener listener) {
        this.mClickListener = listener;
    }

    /**
     * 点击 热图 区域 选中效果
     */
    public interface OnClickListener {
        public void OnClick(View view, HotArea hotArea);
    }
}
