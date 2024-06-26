/*
 * Copyright (C) 2019 CW Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.ListNote.tabs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cw.ListNote.R;
import com.cw.ListNote.db.DB_folder;
import com.cw.ListNote.db.DB_page;
import com.cw.ListNote.drawer.Drawer;
import com.cw.ListNote.folder.FolderUi;
import com.cw.ListNote.main.MainAct;
import com.cw.ListNote.page.Page_recycler;
import com.cw.ListNote.util.ColorSet;
import com.cw.ListNote.util.Util;
import com.cw.ListNote.util.preferences.Pref;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class TabsHost extends AppCompatDialogFragment implements TabLayout.OnTabSelectedListener
{
    public static TabLayout mTabLayout;
    public static ViewPager mViewPager;
    public static TabsPagerAdapter mTabsPagerAdapter;
    public static int mFocusPageTableId;
    public static int mFocusTabPos;

    public static int lastPageTableId;

    public static int firstPos_pageId;

    public static boolean isDoingMarking;
    public TabsHost()
    {
//        System.out.println("TabsHost / construct");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        System.out.println("TabsHost / _onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        System.out.println("TabsHost / _onCreateView");

        View rootView;

        // set layout by orientation
        if (Util.isLandscapeOrientation(MainAct.mAct)) {
            rootView = inflater.inflate(R.layout.tabs_host_landscape, container, false);
        }
        else {
            rootView = inflater.inflate(R.layout.tabs_host_portrait, container, false);
        }

        // view pager
        mViewPager = (ViewPager) rootView.findViewById(R.id.tabs_pager);

        // mTabsPagerAdapter
        mTabsPagerAdapter = new TabsPagerAdapter(MainAct.mAct,MainAct.mAct.getSupportFragmentManager());
//        mTabsPagerAdapter = new TabsPagerAdapter(MainAct.mAct,getChildFragmentManager());

        // add pages to mTabsPagerAdapter
        int pageCount = 0;
        if(Drawer.getFolderCount() > 0) {
            pageCount = addPages(mTabsPagerAdapter);
        }

        // show blank folder if no page exists
        if(pageCount == 0) {
            rootView.findViewById(R.id.blankFolder).setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
        }
        else {
            rootView.findViewById(R.id.blankFolder).setVisibility(View.GONE);
            mViewPager.setVisibility(View.VISIBLE);
        }

        // set mTabsPagerAdapter of view pager
        mViewPager.setAdapter(mTabsPagerAdapter);

        // set tab layout
        mTabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.addOnTabSelectedListener(this);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
//        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

//        mTabLayout.setBackgroundColor(ColorSet.getBarColor(MainAct.mAct));
        mTabLayout.setBackgroundColor(ColorSet.getButtonColor(MainAct.mAct));
//        mTabLayout.setBackgroundColor(Color.parseColor("#FF303030"));

        // tab indicator
        mTabLayout.setSelectedTabIndicatorHeight(15);
        mTabLayout.setSelectedTabIndicatorColor(Color.parseColor("#FFFF7F00"));
//        mTabLayout.setSelectedTabIndicatorHeight((int) (5 * getResources().getDisplayMetrics().density));

        mTabLayout.setTabTextColors(
                ContextCompat.getColor(MainAct.mAct,R.color.colorGray), //normal
                ContextCompat.getColor(MainAct.mAct,R.color.colorWhite) //selected
        );

        mFooterMessage = (TextView) rootView.findViewById(R.id.footerText);
        mFooterMessage.setBackgroundColor(Color.BLUE);
        mFooterMessage.setVisibility(View.VISIBLE);

        return rootView;
    }

    // get MD5
//    private String md5(String md5) {
//        try {
//            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
//            byte[] array = md.digest(md5.getBytes());
//            StringBuffer sb = new StringBuffer();
//            for (int i = 0; i < array.length; ++i) {
//                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
//            }
//            return sb.toString();
//        } catch (java.security.NoSuchAlgorithmException e) {
//        }
//        return null;
//    }

    /**
     * Add pages
     */
    private int addPages(TabsPagerAdapter adapter)
    {
        lastPageTableId = 0;
        int pageCount = adapter.dbFolder.getPagesCount(true);
        System.out.println("TabsHost / _addPages / pagesCount = " + pageCount);

        if(pageCount > 0) {
            for (int i = 0; i < pageCount; i++) {
                int pageTableId = adapter.dbFolder.getPageTableId(i, true);

                if (i == 0)
                    setFirstPos_pageId(adapter.dbFolder.getPageId(i, true));

                if (pageTableId > lastPageTableId)
                    lastPageTableId = pageTableId;

                Page_recycler page = new Page_recycler();
                Bundle args = new Bundle();
                args.putInt("page_pos",i);
                args.putInt("page_table_id",pageTableId);
                page.setArguments(args);
                System.out.println("TabsHost / _addPages / page_tableId = " + pageTableId);
                adapter.addFragment(page);
            }
        }

        return pageCount;
    }

    /**
     * Get last page table Id
     */
    public static int getLastPageTableId()
    {
        return lastPageTableId;
    }

    /**
     * Set last page table Id
     */
    public static void setLastPageTableId(int id)
    {
        lastPageTableId = id;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        System.out.println("TabsHost / _onTabSelected / tab position: " + tab.getPosition());

        // TODO
        //  note: tab position is kept after importing new XML, how to change it?
        setFocus_tabPos(tab.getPosition());

        // keep focus view page table Id
        int pageTableId = mTabsPagerAdapter.dbFolder.getPageTableId(getFocus_tabPos(), true);
        Pref.setPref_focusView_page_tableId(MainAct.mAct, pageTableId);

        // current page table Id
        setCurrentPageTableId(pageTableId);

        // refresh list view of selected page
        Page_recycler page = mTabsPagerAdapter.fragmentList.get(getFocus_tabPos());

        // add for update page item view
        if((page != null) && (page.itemAdapter != null))
        {
            page.itemAdapter.notifyDataSetChanged();
            System.out.println("TabsHost / _onTabSelected / notifyDataSetChanged ");
        }
        else
            System.out.println("TabsHost / _onTabSelected / not notifyDataSetChanged ");

        // set tab high light if implemented
//        tab.setCustomView(null);

        // call onCreateOptionsMenu
        MainAct.mAct.invalidateOptionsMenu();

        // set text color
        mTabLayout.setTabTextColors(
                ContextCompat.getColor(MainAct.mAct,R.color.colorGray), //normal
                ContextCompat.getColor(MainAct.mAct,R.color.colorWhite) //selected
        );

        // set long click listener
        setLongClickListener();

        TabsHost.showFooter(MainAct.mAct);

        isDoingMarking = false;
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    @Override
    public void onResume() {
        super.onResume();
        // default
        setFocus_tabPos(0);

        if(Drawer.getFolderCount() == 0)
            return;//todo Check again

        // restore focus view page
        int pageCount = mTabsPagerAdapter.dbFolder.getPagesCount(true);
        System.out.println("TabsHost / _onResume / pageCount = " + pageCount);
        for(int pos=0; pos<pageCount; pos++)
        {
            int pageTableId = mTabsPagerAdapter.dbFolder.getPageTableId(pos, true);

            if(pageTableId == Pref.getPref_focusView_page_tableId(MainAct.mAct)) {
                System.out.println("TabsHost / _onResume / set focus tab pos = " + pos);
                setFocus_tabPos(pos);
                setCurrentPageTableId(pageTableId);
            }
        }

        System.out.println("TabsHost / _onResume / _getFocus_tabPos = " + getFocus_tabPos());

        // auto scroll to show focus tab
        new Handler().postDelayed(
                new Runnable() {
                    @Override public void run() {
                        if(mTabLayout.getTabAt(getFocus_tabPos()) != null)
                            mTabLayout.getTabAt(getFocus_tabPos()).select();
                    }
                }, 100);

        // set long click listener
        setLongClickListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("TabsHost / _onPause");

        //  Remove fragments
        if(!MainAct.mAct.isDestroyed())
            removeTabs();//Put here will solve onBackStackChanged issue (no Page_recycler / _onCreate)
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // store scroll of recycler view
    public static void store_listView_vScroll(RecyclerView recyclerView)
    {
        int firstVisibleIndex = ((LinearLayoutManager) recyclerView.getLayoutManager())
                .findFirstVisibleItemPosition();

        View v = recyclerView.getChildAt(0);
        int firstVisibleIndexTop = (v == null) ? 0 : v.getTop();

        System.out.println("TabsHost / _store_listView_vScroll / firstVisibleIndex = " + firstVisibleIndex +
                " , firstVisibleIndexTop = " + firstVisibleIndexTop);

        // keep index and top position
        Pref.setPref_focusView_list_view_first_visible_index(MainAct.mAct, firstVisibleIndex);
        Pref.setPref_focusView_list_view_first_visible_index_top(MainAct.mAct, firstVisibleIndexTop);
    }

    // resume scroll of recycler view
    public static void resume_listView_vScroll(RecyclerView recyclerView)
    {
        // recover scroll Y
        int firstVisibleIndex = Pref.getPref_focusView_list_view_first_visible_index(MainAct.mAct);
        int firstVisibleIndexTop = Pref.getPref_focusView_list_view_first_visible_index_top(MainAct.mAct);

        System.out.println("TabsHost / _resume_listView_vScroll / firstVisibleIndex = " + firstVisibleIndex +
                " , firstVisibleIndexTop = " + firstVisibleIndexTop);

        // restore index and top position
        ((LinearLayoutManager)recyclerView.getLayoutManager()).scrollToPositionWithOffset(firstVisibleIndex, firstVisibleIndexTop);
    }


    /**
     * Get first position page Id
     * @return page Id of 1st position
     */
    public static int getFirstPos_pageId() {
        return firstPos_pageId;
    }

    /**
     * Set first position table Id
     * @param id: page Id
     */
    public static void setFirstPos_pageId(int id) {
        firstPos_pageId = id;
    }

    public static void reloadCurrentPage()
    {
        System.out.println("TabsHost / _reloadCurrentPage");
        int pagePos = getFocus_tabPos();
        mViewPager.setAdapter(mTabsPagerAdapter);
        mViewPager.setCurrentItem(pagePos);
    }

    public static Page_recycler getCurrentPage()
    {
        return mTabsPagerAdapter.fragmentList.get(getFocus_tabPos());
    }


    public static void setCurrentPageTableId(int id)
    {
        System.out.println("TabsHost / _setCurrentPageTableId / id = " + id);
        mFocusPageTableId = id;
    }


    public static int getCurrentPageTableId()
    {
        System.out.println("TabsHost / _getCurrentPageTableId / mFocusPageTableId = " + mFocusPageTableId);
        return mFocusPageTableId;
    }


    /**
     * Set long click listeners for tabs editing
     */
    void setLongClickListener()
    {
//        System.out.println("TabsHost / _setLongClickListener");

        //https://stackoverflow.com/questions/33367245/add-onlongclicklistener-on-android-support-tablayout-tablayout-tab
        // on long click listener
        LinearLayout tabStrip = (LinearLayout) mTabLayout.getChildAt(0);
        final int tabsCount =  tabStrip.getChildCount();
        for (int i = 0; i < tabsCount; i++)
        {
            final int tabPos = i;
            tabStrip.getChildAt(tabPos).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    editPageTitle(tabPos,MainAct.mAct);
                    return false;
                }
            });
        }
    }

    /**
     * edit page title
     *
     */
    static void editPageTitle(final int tabPos, final AppCompatActivity act)
    {
        final DB_folder mDbFolder = mTabsPagerAdapter.dbFolder;

        // get tab name
        String title = mDbFolder.getPageTitle(tabPos, true);

        final EditText editText1 = new EditText(act.getBaseContext());
        editText1.setText(title);
        editText1.setSelection(title.length()); // set edit text start position
        editText1.setTextColor(Color.BLACK);

        //update tab info
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(R.string.edit_page_tab_title)
                .setMessage(R.string.edit_page_tab_message)
                .setView(editText1)
                .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
                                    {   @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {/*cancel*/}
                                    })
                .setNeutralButton(R.string.edit_page_button_delete, new DialogInterface.OnClickListener()
                    {   @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // delete
                            Util util = new Util(act);
                            util.vibrate();

                            AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
                            builder1.setTitle(R.string.confirm_dialog_title)
                                    .setMessage(R.string.confirm_dialog_message_page)
                                    .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog1, int which1){
                                            /*nothing to do*/}})
                                    .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog1, int which1){
                                            deletePage(tabPos, act);
                                            FolderUi.selectFolder(act,FolderUi.getFocus_folderPos());
                                        }})
                                    .show();
                        }
                    })
                .setPositiveButton(R.string.edit_page_button_update, new DialogInterface.OnClickListener()
                    {   @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // save
                            final int pageId =  mDbFolder.getPageId(tabPos, true);
                            final int pageTableId =  mDbFolder.getPageTableId(tabPos, true);

                            int tabStyle = mDbFolder.getPageStyle(tabPos, true);
                            mDbFolder.updatePage(pageId,
                                                 editText1.getText().toString(),
                                                 pageTableId,
                                                 tabStyle,
                                                 true);

                            FolderUi.startTabsHostRun();
                        }
                    })
                .setIcon(android.R.drawable.ic_menu_edit);

        AlertDialog d1 = builder.create();
        d1.show();
        // android.R.id.button1 for positive: save
        ((Button)d1.findViewById(android.R.id.button1))
                .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);

        // android.R.id.button2 for negative: color
        ((Button)d1.findViewById(android.R.id.button2))
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

        // android.R.id.button3 for neutral: delete
        ((Button)d1.findViewById(android.R.id.button3))
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);

    }

    /**
     * delete page
     *
     */
    public static  void deletePage(int tabPos, final AppCompatActivity activity)
    {

        final DB_folder mDbFolder = mTabsPagerAdapter.dbFolder;
        int pageId =  mDbFolder.getPageId(tabPos, true);

        // check if only one page left
        int pagesCount = mDbFolder.getPagesCount(true);
        int mFirstPos_PageId = 1;

        mDbFolder.open();
        Cursor mPageCursor = mDbFolder.getPageCursor();
        if(mPageCursor.isFirst())
            mFirstPos_PageId = pageId;
        mDbFolder.close();

        if(pagesCount > 0)
        {
            //if current page is the first page and will be delete,
            //try to get next existence of note page
            System.out.println("TabsHost / deletePage / tabPos = " + tabPos);
            System.out.println("TabsHost / deletePage / mFirstPos_PageId = " + mFirstPos_PageId);
            if(pageId == mFirstPos_PageId)
            {
                int cGetNextExistIndex = getFocus_tabPos() +1;
                boolean bGotNext = false;
                while(!bGotNext){
                    try{
                        mFirstPos_PageId =  mDbFolder.getPageId(cGetNextExistIndex, true);
                        bGotNext = true;
                    }catch(Exception e){
                        bGotNext = false;
                        cGetNextExistIndex++;
                    }
                }
            }

            //change to first existing page
            mDbFolder.open();
            for(int i=0 ; i<pagesCount; i++)
            {
                if(	mDbFolder.getPageId(i, false)== mFirstPos_PageId)
                {
                    int newFirstPageTblId =  mDbFolder.getPageTableId(i, false);
                    System.out.println("TabsHost / deletePage / newFirstPageTblId = " + newFirstPageTblId);
                    Pref.setPref_focusView_page_tableId(activity, newFirstPageTblId);
                }
            }
            mDbFolder.close();
        }
//		else
//		{
//             Toast.makeText(activity, R.string.toast_keep_one_page , Toast.LENGTH_SHORT).show();
//             return;
//		}

        // get page table Id for dropping
        int pageTableId = mDbFolder.getPageTableId(tabPos, true);
        System.out.println("TabsHost / _deletePage / pageTableId =  " + pageTableId);

        // delete tab name
        mDbFolder.dropPageTable(pageTableId,true);
        mDbFolder.deletePage(DB_folder.getFocusFolder_tableName(),pageId,true);

        // update change after deleting tab
        FolderUi.startTabsHostRun();
    }

    public static TextView mFooterMessage;

    // set footer
    public static void showFooter(AppCompatActivity mAct)
    {
//		System.out.println("TabsHost / _showFooter ");

        // show footer
        mFooterMessage.setTextColor(ColorSet.color_white);
        if(mFooterMessage != null)
        {
            mFooterMessage.setText(getFooterMessage(mAct));
            mFooterMessage.setBackgroundColor(ColorSet.getBarColor(mAct));
        }
    }

    // get footer message of list view
    static String getFooterMessage(AppCompatActivity mAct)
    {
        int pageTableId = Pref.getPref_focusView_page_tableId(mAct);
        DB_page mDb_page = new DB_page(mAct, pageTableId);
        return mAct.getResources().getText(R.string.footer_checked).toString() +
                "/" +
                mAct.getResources().getText(R.string.footer_total).toString() +
                ": " +
                mDb_page.getCheckedNotesCount() +
                "/" +
                mDb_page.getNotesCount(true);
    }

    /**
     * Get focus tab position
    */
    public static int getFocus_tabPos()
    {
        return mFocusTabPos;
    }

    /**
     * Set focus tab position
     * @param pos
     */
    public static void setFocus_tabPos(int pos)
    {
        mFocusTabPos = pos;
    }


    public static void removeTabs()
    {
        System.out.println("TabsHost / _removeTabs");
    	if(TabsHost.mTabsPagerAdapter == null)
    		return;

        ArrayList<Page_recycler> fragmentList = TabsHost.mTabsPagerAdapter.fragmentList;
        if( (fragmentList != null) &&
            (fragmentList.size() >0)  )
        {
            RecyclerView listView = fragmentList.get(TabsHost.getFocus_tabPos()).recyclerView;//drag_listView;

            if(listView != null)
                TabsHost.store_listView_vScroll(listView);

            for (int i = 0; i < fragmentList.size(); i++) {
                System.out.println("TabsHost / _removeTabs / i = " + i);
                MainAct.mAct.getSupportFragmentManager().beginTransaction().remove(fragmentList.get(i)).commit();
            }
        }
    }

}