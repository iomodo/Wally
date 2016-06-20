package com.wally.wally.tango;

import android.util.Log;

import com.bumptech.glide.util.Util;
import com.projecttango.rajawali.Pose;
import com.wally.wally.R;
import com.wally.wally.Utils;
import com.wally.wally.datacontroller.content.Content;
import com.wally.wally.datacontroller.content.TangoData;

import org.rajawali3d.Object3D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.wally.wally.tango.VisualContent.*;

/**
 * Created by shota on 5/30/16.
 */
public class VisualContentManager implements LocalizationListener {
    private static final String TAG = VisualContentManager.class.getSimpleName();
    private boolean mIsLocalized;
    private final Object mLocalizationLock = new Object();

    //Active Content
    private final Object mActiveContentLock = new Object();
    private ActiveVisualContent mActiveContent;
    private ActiveVisualContent mSavedActiveContent;

    //Static Content
    private final Object mStaticContentLock = new Object();
    private List<VisualContent> mStaticContent;
    private List<VisualContent> mSavedStaticContent;

    //Selected Content
    private final Object mSelectedContentLock = new Object();
    private VisualContent mSelectedContent;
    private boolean mBorderOnScreen;


    public VisualContentManager() {
        mIsLocalized = false;
        mStaticContent = new ArrayList<>();
        mSavedStaticContent = new ArrayList<>();
    }

    /**
     * Is called from TangoUpdater through LocalizationListener
     * indicates localized tango device
     */
    @Override
    public void localized() {
        Log.d(TAG, "localized() called with: " + "");
        synchronized (mLocalizationLock) {
            mIsLocalized = true;
            synchronized (mActiveContentLock){
                if (mSavedActiveContent != null) {
                    mActiveContent = getLocalizationNewActiveContent(mSavedActiveContent, mActiveContent);
                    mSavedActiveContent = null;
                }
            }
            synchronized (mStaticContentLock) {
                if (mSavedStaticContent != null) {
                    mStaticContent = getLocalizationNewStaticContent(mSavedStaticContent, mStaticContent);
                    mSavedStaticContent = null;
                }
            }
        }
    }

    private ActiveVisualContent getLocalizationNewActiveContent(ActiveVisualContent savedContent, ActiveVisualContent contentNow){
        ActiveVisualContent res = savedContent;
        if ((savedContent.getStatus() == RenderStatus.Rendered || savedContent.getStatus() == RenderStatus.PendingRender)
                && (contentNow == null || (contentNow.getStatus() != RenderStatus.Rendered && contentNow.getStatus() != RenderStatus.PendingRemove))) {
            res.setStatus(RenderStatus.PendingRender);
        }

        return res;
    }

    /**
     * Is called from TangoUpdater through LocalizationListener
     * indicates not localized tango device
     */
    @Override
    public void notLocalized() {
        Log.d(TAG, "notLocalized() called with: " + "");
        synchronized (mLocalizationLock) {
            mIsLocalized = false;
            synchronized (mActiveContentLock){
                if (isActiveContent()) {
                    mSavedActiveContent = mActiveContent.cloneContent();
                    removePendingActiveContent();
                }
            }
            synchronized (mStaticContentLock) {
                mSavedStaticContent = cloneList(mStaticContent);
                removeAllStaticContent();
            }
        }
    }

    /**
     * Is called from Renderer thread
     *
     * @return
     */
    public boolean isLocalized() {
        synchronized (mLocalizationLock) {
            return mIsLocalized;
        }
    }

    private List<VisualContent> cloneList(List<VisualContent> list) {
        List<VisualContent> res = new ArrayList<>();
        for (VisualContent vc : list) {
            res.add(vc.cloneContent());
        }
        return res;
    }


    private VisualContent findVisualContentInStaticContentList(VisualContent vc, List<VisualContent> contentNow){
        for(VisualContent c: contentNow){
            if (c.getContent().equals(vc.getContent())) return c;
        }
        return null;
    }

    private ArrayList<VisualContent> getLocalizationNewStaticContent(List<VisualContent> savedContent, List<VisualContent> contentNow){
        ArrayList<VisualContent> newCon = new ArrayList<>();
        for (VisualContent oldC : savedContent){
            VisualContent newC = findVisualContentInStaticContentList(oldC, contentNow);
            if (newC != null){
                if((oldC.getStatus() == RenderStatus.Rendered || oldC.getStatus() == RenderStatus.PendingRender) &&
                        (newC.getStatus() != RenderStatus.Rendered && newC.getStatus() != RenderStatus.PendingRemove)){
                    oldC.setStatus(RenderStatus.PendingRender);
                    newCon.add(oldC);
                } else if (oldC.getStatus() == RenderStatus.PendingRemove && newC.getStatus() != RenderStatus.None){
                    newCon.add(oldC);
                }
            } else {
                if (oldC.getStatus() == RenderStatus.Rendered){
                    oldC.setStatus(RenderStatus.PendingRender);
                    newCon.add(oldC);
                } else if (oldC.getStatus() == RenderStatus.PendingRender){
                    newCon.add(oldC);
                }
            }
        }
        return newCon;
    }

    private void removeAllStaticContent() {
        //TODO buggy when renderer gets pending staticContent it will be rendered anyway.
        synchronized (mStaticContentLock) {
            for (VisualContent vc : mStaticContent) {
                if (vc.getStatus() == RenderStatus.Rendered) {
                    vc.setStatus(RenderStatus.PendingRemove);
                } else if (vc.getStatus() == RenderStatus.PendingRender) {
                    vc.setStatus(RenderStatus.None);
                }
            }
        }
    }

    /****************************************** Active Content ******************************************/

    /**
     * Creates Active Content with @RenderStatus.PendingRender status. Is called from ContentFitter thread.
     *
     * @param glPose
     * @param content
     */
    public void addPendingActiveContent(Pose glPose, Content content) {
        synchronized (mActiveContentLock) {
            if (mActiveContent == null) {
                content.withTangoData(new TangoData(glPose));
                this.mActiveContent = new ActiveVisualContent(content);
                mActiveContent.setStatus(RenderStatus.PendingRender);
            } else {
                Utils.throwError();
            }
        }
    }

    public void updateActiveContent(Pose newPose) {
        synchronized (mActiveContentLock) {
            if (mActiveContent != null) {
                mActiveContent.setNewPose(newPose);
            } else {
                Utils.throwError();
            }
        }
    }

    public void scaleActiveContent(float scale){
        synchronized (mActiveContent){
            if (mActiveContent != null){
                mActiveContent.scaleContent(scale);
            } else {
                Utils.throwError();
            }
        }
    }

    public void removePendingActiveContent() {
        synchronized (mActiveContentLock) {
            if (mActiveContent != null) {
                if (mActiveContent.getStatus() == RenderStatus.Rendered) {
                    mActiveContent.setStatus(RenderStatus.PendingRemove);
                } else if (mActiveContent.getStatus() == RenderStatus.PendingRender) {
                    mActiveContent.setStatus(RenderStatus.None);
                }
            } else {
                Utils.throwError();
            }
        }
    }

    /**
     * Is called when renderer renders active content
     */
    public void setActiveContentAdded() {
        synchronized (mActiveContentLock) {
            if (mActiveContent != null) {
                mActiveContent.setStatus(RenderStatus.Rendered);
            } else {
                Utils.throwError();
            }
        }
    }

    public void setActiveContentFinishFitting() {
        synchronized (mActiveContentLock) {
            synchronized (mStaticContentLock) {
                if (mActiveContent.getStatus() == RenderStatus.Rendered) {
                    int index = mStaticContent.indexOf(mActiveContent);
                    if (index == -1) {
                        mStaticContent.add(mActiveContent);
                    } else {
                        mStaticContent.set(index, mActiveContent);
                    }
                    mActiveContent = null;
                } else {
                    Utils.throwError();
                }
            }
        }
    }

    public void setActiveContentRemoved() {
        synchronized (mActiveContentLock) {
            if (mActiveContent.getStatus() == RenderStatus.PendingRemove || mActiveContent.getStatus() == RenderStatus.None) {
                mActiveContent = null;
            } else {
                Utils.throwError();
            }
        }
    }

    public ActiveVisualContent getActiveContent() {
        synchronized (mActiveContentLock) {
            return mActiveContent;
        }
    }

    public boolean isActiveContent(){
        synchronized (mActiveContentLock){
            return mActiveContent != null;
        }
    }

    /**
     * Is called from Render thread. So renderer can decide to add active content
     *
     * @return
     */
    public boolean shouldActiveContentRenderOnScreen() {
        synchronized (mLocalizationLock) {
            synchronized (mActiveContentLock) {
                return mActiveContent != null && mActiveContent.getStatus() == RenderStatus.PendingRender && isLocalized();
            }
        }
    }

    /**
     * Is called from Render thread. So renderer can decide to remove rendered active content
     *
     * @return
     */
    public boolean shouldActiveContentRemoveFromScreen() {
        synchronized (mActiveContentLock) {
            return mActiveContent != null && mActiveContent.getStatus() == RenderStatus.PendingRemove;
        }
    }


    /******************************************
     * Static Content
     ******************************************/

    public void createStaticContent(final Collection<Content> collection) {
        synchronized (mLocalizationLock) {
            synchronized (mStaticContentLock) {
                if (isLocalized()) {
                    Log.d(TAG, "createStaticContent() isLocalized");
                    for (Content c : collection) {
                        addPendingStaticContent(new VisualContent(c));
                    }

                } else {
                    Log.d(TAG, "createStaticContent() is not Localized");
                    for (Content c : collection) {
                        addSavedPendingStaticContent(new VisualContent(c));
                    }
                }
            }
        }
    }

    public void removePendingStaticContent(Content content) {
        synchronized (mStaticContentLock) {
            VisualContent vc = findVisualContentByContent(content);
            removePendingStaticContent(vc);
        }
    }

    public void setStaticContentAdded(VisualContent visualContent) {
        synchronized (mStaticContentLock) {
            visualContent.setStatus(RenderStatus.Rendered);
            int index = mStaticContent.indexOf(visualContent);
            if (index != -1) {
                mStaticContent.set(index, visualContent);
            } else {
                Utils.throwError();

            }
        }
    }

    public void setStaticContentRemoved(VisualContent visualContent) {
        synchronized (mStaticContentLock) {
            int index = mStaticContent.indexOf(visualContent);
            if (index != -1) {
                mStaticContent.remove(visualContent);
            } else {
                Utils.throwError();
            }
        }
    }

    public Iterator<VisualContent> getStaticVisualContentToAdd() {
        synchronized (mLocalizationLock) {
            synchronized (mStaticContentLock) {
                if (isLocalized()) {
                    return filterStaticVisualContentList(RenderStatus.PendingRender);
                } else {
                    return Collections.emptyIterator();
                }
            }
        }
    }

    public Iterator<VisualContent> getStaticVisualContentToRemove() {
        synchronized (mStaticContentLock) {
            return filterStaticVisualContentList(RenderStatus.PendingRemove);
        }
    }

    public VisualContent findVisualContentByContent(Content content) {
        synchronized (mStaticContentLock) {
            for (VisualContent vc : mStaticContent) {
                if (vc.getContent().equals(content)) {
                    return vc;
                }
            }
            return null;
        }
    }

    public VisualContent findContentByObject3D(Object3D object) {
        synchronized (mStaticContentLock) {
            for (VisualContent vc : mStaticContent) {
                if (vc.getVisual().equals(object) && vc.getStatus() == RenderStatus.Rendered) {
                    return vc;
                }
            }
        }
        return null;
    }

    private Iterator<VisualContent> filterStaticVisualContentList(RenderStatus status) {
        List<VisualContent> res = new ArrayList<>();
        for (VisualContent vc : mStaticContent) {
            if (vc.getStatus() == status) {
                res.add(vc);
            }
        }
        return res.iterator();
    }


    private void addPendingStaticContent(VisualContent visualContent) {
        synchronized (mStaticContentLock) {
            visualContent.setStatus(RenderStatus.PendingRender);
            int index = mStaticContent.indexOf(visualContent);
            if (index == -1) {
                mStaticContent.add(visualContent);
            } else {
                mStaticContent.set(index, visualContent);
            }
        }
    }

    private void addSavedPendingStaticContent(VisualContent visualContent) {
        synchronized (mStaticContentLock) {
            visualContent.setStatus(RenderStatus.PendingRender);
            int index = mSavedStaticContent.indexOf(visualContent);
            if (index == -1) {
                mSavedStaticContent.add(visualContent);
            } else {
                mSavedStaticContent.set(index, visualContent);
            }
        }
    }

    private void removePendingStaticContent(VisualContent visualContent) {
        synchronized (mStaticContentLock) {
            visualContent.setStatus(RenderStatus.PendingRemove);
            int index = mStaticContent.indexOf(visualContent);
            if (index == -1) {
            } else {
                mStaticContent.set(index, visualContent);
            }
        }
    }

    /******************************************
     * Selected Content
     ******************************************/

    public void setSelectedContent(VisualContent visualContent) {
        synchronized (mSelectedContentLock) {
            mSelectedContent = visualContent;
        }
    }

    public boolean isSelectedContent(VisualContent visualContent) {
        synchronized (mSelectedContentLock) {
            return visualContent == mSelectedContent;
        }
    }

    public boolean isBorderOnScreen() {
        synchronized (mSelectedContentLock) {
            return mBorderOnScreen;
        }
    }

    public void setBorderOnScreen(boolean borderOnScreen) {
        synchronized (mSelectedContentLock) {
            this.mBorderOnScreen = borderOnScreen;
        }
    }

}
