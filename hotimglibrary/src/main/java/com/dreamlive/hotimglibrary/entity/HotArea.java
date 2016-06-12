package com.dreamlive.hotimglibrary.entity;

import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.text.TextUtils;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片点击区域对应的实体类
 * Created by dreamlivemeng on 2016/6/7.
 */
public class HotArea implements Serializable {
    private static final long serialVersionUID = -2129399099105307178L;
    /**
     * 区域id
     */
    private String areaId;
    /**
     * 区域名称
     */
    private String areaTitle;
    /**
     * 区域介绍
     */
    private String desc;
    /***
     * 区域点坐标
     */
    private int[] pts;
    private List<HotArea> areas = new ArrayList<HotArea>();

    public HotArea() {
    }

    public HotArea(String areaId, String areaTitle, String desc, int[] pts) {
        this.areaId = areaId;
        this.areaTitle = areaTitle;
        this.desc = desc;
        this.pts = pts;
    }

    public HotArea(String areaId, String areaTitle, String desc, String[] pts) {
        this.areaId = areaId;
        this.areaTitle = areaTitle;
        this.desc = desc;
        setStrArrayToIntArray(pts);
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaTitle() {
        return areaTitle;
    }

    public void setAreaTitle(String areaTitle) {
        this.areaTitle = areaTitle;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int[] getPts() {
        return pts;
    }

    public void setPts(int[] pts) {
        this.pts = pts;
    }

    public List<HotArea> getAreas() {
        return areas;
    }

    public void setAreas(List<HotArea> areas) {
        this.areas = areas;
    }

    public CheckArea getCheckArea() {
        CheckArea checkArea = null;
        if (pts != null) {
            checkArea = new CheckArea(pts);
        }
        return checkArea;
    }

    public void setPts(String[] pts) {
        setStrArrayToIntArray(pts);
    }

    private void setStrArrayToIntArray(String[] pts) {
        if (null != pts && 0 != pts.length) {
            int len = pts.length;
            this.pts = new int[len];
            for (int i = 0; i < len; ++i) {
                try {
                    this.pts[i] = Integer.parseInt(pts[i]);
                } catch (Exception e) {
                    this.pts[i] = 0;
                    Log.e("SmartPit", e.getMessage());
                }
            }
        }
    }

    public void setPts(String pts, String split) {
        if (!TextUtils.isEmpty(pts) && !TextUtils.isEmpty(split)) {
            String[] points = pts.split(split);
            setPts(points);
        }
    }

    /**
     * 写内部类的原因在于在Activity之间传递时，
     * 不能传未实现Serializable接口的类
     */
    public class CheckArea {
        private final Path path;
        //当前处理是从点的个数来判断是矩形 还是多边形,这两种的方式对点的位置判断不太一样
        private final boolean isRectF;

        private CheckArea(int[] pts) {
            this.path = new Path();
            int len = pts.length;
            isRectF = len == 4;
            Log.e("TAG", "len================" + len);
            for (int i = 0; i < len; ) {
                if (i == 0) {
                    this.path.moveTo(pts[i++], pts[i++]);
                } else {
                    this.path.lineTo(pts[i++], pts[i++]);
                }
            }
            this.path.close();
        }

        public Path getPath() {
            return this.path;
        }

        /**
         * 检测是否在区域范围内
         *
         * @param rectf 从外部传可以重用
         * @param x
         * @param y
         * @return
         */
        public boolean isInArea(RectF rectf, float x, float y) {
            boolean resStatus = false;
            if (this.path != null) {
                rectf.setEmpty();
                path.computeBounds(rectf, true);
                if (isRectF) {
                    //当是矩形时
                    resStatus = rectf.contains(x, y);
                } else {
                    //如果是多边形时
                    Region region = new Region();
                    region.setPath(path, region);
                    region.setPath(path, new Region((int) rectf.left, (int) rectf.top, (int) rectf.right, (int) rectf.bottom));
                    resStatus = region.contains((int) x, (int) y);
                }
            }
            return resStatus;
        }
    }
}
