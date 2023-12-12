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

package com.cw.ListNote.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.cw.ListNote.R;
import com.cw.ListNote.config.About;
import com.cw.ListNote.config.Config;
import com.cw.ListNote.db.DB_folder;
import com.cw.ListNote.db.DB_page;
import com.cw.ListNote.drawer.Drawer;
import com.cw.ListNote.folder.Folder;
import com.cw.ListNote.folder.FolderUi;
import com.cw.ListNote.note_add.Add_note_option;
import com.cw.ListNote.operation.delete.DeleteFolders;
import com.cw.ListNote.operation.delete.DeletePages;
import com.cw.ListNote.page.Checked_notes_option;
import com.cw.ListNote.page.PageUi;
import com.cw.ListNote.tabs.TabsHost;
import com.cw.ListNote.util.DeleteFileAlarmReceiver;
import com.cw.ListNote.operation.import_export.Export_toSDCardFragment;
import com.cw.ListNote.operation.import_export.Import_filesList;
import com.cw.ListNote.db.DB_drawer;
import com.cw.ListNote.define.Define;
import com.cw.ListNote.operation.mail.MailNotes;
import com.cw.ListNote.util.OnBackPressedListener;
import com.cw.ListNote.operation.mail.MailPagesFragment;
import com.cw.ListNote.util.Util;
import com.cw.ListNote.util.preferences.Pref;
import com.mobeta.android.dslv.DragSortListView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;

public class MainAct extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener
{
    public static CharSequence mFolderTitle;
    public static CharSequence mAppTitle;
    public Context mContext;
    public Config mConfigFragment;
    public About mAboutFragment;
    public static Menu mMenu;
    public static List<String> mFolderTitles;
    public static AppCompatActivity mAct;//TODO static issue
    public FragmentManager mFragmentManager;
    public static FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener;
    public static int mLastOkTabId = 1;
    public static SharedPreferences mPref_show_note_attribute;
    OnBackPressedListener onBackPressedListener;
    public Drawer drawer;
    public static Folder mFolder;
    public static MainUi mMainUi;
    public static Toolbar mToolbar;

    public static MediaBrowserCompat mMediaBrowserCompat;
    public static MediaControllerCompat mMediaControllerCompat;
    public static int mCurrentState;
    public final static int STATE_PAUSED = 0;
    public final static int STATE_PLAYING = 1;
//    public boolean bEULA_accepted;

	// Main Act onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Define.setAppBuildMode();

        // Release mode: no debug message
        if (Define.CODE_MODE == Define.RELEASE_MODE) {
            OutputStream nullDev = new OutputStream() {
                public void close() {}
                public void flush() {}
                public void write(byte[] b) {}
                public void write(byte[] b, int off, int len) {}
                public void write(int b) {}
            };
            System.setOut(new PrintStream(nullDev));
        }

        System.out.println("================start application ==================");
        System.out.println("MainAct / _onCreate");

        mAct = this;
        mAppTitle = getTitle();
        mMainUi = new MainUi();

        // add the following to disable this requirement
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                // method 1
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);

                // method 2
//                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//                StrictMode.setVmPolicy(builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Show Api version
        if (Define.CODE_MODE == Define.DEBUG_MODE)
            Toast.makeText(this, mAppTitle + " " + "API_" + Build.VERSION.SDK_INT, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, mAppTitle, Toast.LENGTH_SHORT).show();

        if(Pref.getPref_will_create_default_content(this))
            createDefaultContent_byInitialTables();
        else
            doCreate();
    }

    // Do major create operation
    void doCreate()
    {
        System.out.println("MainAct / _doCreate");

        mFolderTitles = new ArrayList<>();

        //Add note with the link which got from other App
        String intentLink = mMainUi.addNote_IntentLink(getIntent(), mAct);
        if (!Util.isEmptyString(intentLink)) {
            finish(); // ListNote not running at first, keep closing
            return;
        } else {
            // check DB
            final boolean ENABLE_DB_CHECK = false;//true;//false
            if (ENABLE_DB_CHECK) {
                // list all folder tables
                FolderUi.listAllFolderTables(mAct);

                // recover focus
                DB_folder.setFocusFolder_tableId(Pref.getPref_focusView_folder_tableId(this));
                DB_page.setFocusPage_tableId(Pref.getPref_focusView_page_tableId(this));
            }//if(ENABLE_DB_CHECK)

            mContext = getBaseContext();

            // add on back stack changed listener
            mFragmentManager = getSupportFragmentManager();
            mOnBackStackChangedListener = this;
            mFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);
        }

        isAddedOnNewIntent = false;

        configLayoutView(); //createAssetsFile inside
    }


    /**
     * Create initial tables
     */
    void createDefaultContent_byInitialTables(){
        DB_drawer dB_drawer = new DB_drawer(this);

        for(int i = 1; i<= Define.INITIAL_FOLDERS_COUNT; i++){
            // Create initial folder tables
            System.out.println("MainAct / _createInitialTables / folder id = " + i);
            String folderTitle = getResources().getString(R.string.default_folder_name).concat(String.valueOf(i));
            dB_drawer.insertFolder(i, folderTitle, true); // Note: must set false for DB creation stage
            dB_drawer.insertFolderTable( i, true);

            // Create initial page tables
            if(Define.INITIAL_PAGES_COUNT > 0)
            {
                // page tables
                for(int j = 1; j<= Define.INITIAL_PAGES_COUNT; j++)
                {
                    System.out.println("MainAct / _createInitialTables / page id = " + j);
                    DB_folder db_folder = new DB_folder(this,i);
                    db_folder.insertPageTable(db_folder, i, j, true);

                    String DB_FOLDER_TABLE_PREFIX = "Folder";
                    String folder_table = DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(i));
                    db_folder.open();
                    db_folder.insertPage(db_folder.mSqlDb ,
                            folder_table,
                            Define.getTabTitle(this,j),
                            1,
                            Define.STYLE_DEFAULT);//Define.STYLE_PREFER
                    db_folder.close();
                    //db_folder.insertPage(sqlDb,folder_table,"N2",2,1);
                }
            }
        }

        recreate();
        Pref.setPref_will_create_default_content(this,false);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
    }

    void initActionBar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            mToolbar.setNavigationIcon(R.drawable.ic_drawer);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Drawer.drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    // set action bar for fragment
    void initActionBar_home() {
        drawer.drawerToggle.setDrawerIndicatorEnabled(false);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayShowHomeEnabled(false);//false: no launcher icon
        }

        mToolbar.setNavigationIcon(R.drawable.ic_menu_back);
        mToolbar.getChildAt(1).setContentDescription(getResources().getString(R.string.btn_back));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("MainAct / _initActionBar_home / click to popBackStack");

                // check if DB is empty
                DB_drawer db_drawer = new DB_drawer(mAct);
                int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(mAct);
                DB_folder db_folder = new DB_folder(mAct,focusFolder_tableId);
                if((db_drawer.getFoldersCount(true) == 0) ||
                   (db_folder.getPagesCount(true) == 0)      )
                {
                    finish();
                    Intent intent  = new Intent(mAct,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                    getSupportFragmentManager().popBackStack();
            }
        });

    }


    /*********************************************************************************
     *
     *                                      Life cycle
     *
     *********************************************************************************/

    boolean isAddedOnNewIntent;
    // if one ListNote Intent is already running, call it again in YouTube or Browser will run into this
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        if(!isAddedOnNewIntent)
        {
            String intentTitle = mMainUi.addNote_IntentLink(intent, mAct);
//            if (!Util.isEmptyString(intentTitle) && intentTitle.startsWith("http")) {
//                Page.itemAdapter.notifyDataSetChanged();
//            }

            if (!Util.isEmptyString(intentTitle))
                TabsHost.reloadCurrentPage();

            if(Build.VERSION.SDK_INT >= O)//API26
                isAddedOnNewIntent = true; // fix 2 times _onNewIntent on API26
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        bluetooth_device_receiver.abortBroadcast();//todo better place?
        System.out.println("MainAct / _onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
    	System.out.println("MainAct / _onResume");
        mAct = this;

        // Sync the toggle state after onRestoreInstanceState has occurred.
//        if(bEULA_accepted)
        {
            if(drawer != null)
                drawer.drawerToggle.syncState();

            // get focus folder table Id, default folder table Id: 1
            DB_drawer dB_drawer = new DB_drawer(this);
            dB_drawer.open();
            for (int i = 0; i < dB_drawer.getFoldersCount(false); i++) {
                if (dB_drawer.getFolderTableId(i, false) == Pref.getPref_focusView_folder_tableId(this)) {
                    FolderUi.setFocus_folderPos(i);
                    System.out.println("MainAct / _mainAction / FolderUi.getFocus_folderPos() = " + FolderUi.getFocus_folderPos());
                }
            }
            dB_drawer.close();
        }
    }


    @Override
    protected void onResumeFragments() {
        System.out.println("MainAct / _onResumeFragments ");
        super.onResumeFragments();

        if(mFragmentManager != null)
            mFragmentManager.popBackStack();

        if (!mAct.isDestroyed()) {
            System.out.println("MainAct / _onResumeFragments / mAct is not Destroyed()");
            openFolder();
        } else
            System.out.println("MainAct / _onResumeFragments / mAct is Destroyed()");
    }

    // open folder
    public static void openFolder(){
        System.out.println("MainAct / _openFolder");

        DB_drawer dB_drawer = new DB_drawer(mAct);
        int folders_count = dB_drawer.getFoldersCount(true);

        if (folders_count > 0) {
            int pref_focus_table_id = Pref.getPref_focusView_folder_tableId(MainAct.mAct);
            for(int folder_pos=0; folder_pos<folders_count; folder_pos++)
            {
                if(dB_drawer.getFolderTableId(folder_pos,true) == pref_focus_table_id) {
                    // select folder
                    FolderUi.selectFolder(mAct, folder_pos);

                    // set focus folder position
                    FolderUi.setFocus_folderPos(folder_pos);
                }
            }
            // set focus table Id
            DB_folder.setFocusFolder_tableId(pref_focus_table_id);

            if (mAct.getSupportActionBar() != null)
                mAct.getSupportActionBar().setTitle(mFolderTitle);
        }
    }


    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("MainAct / _onConfigurationChanged");

        configLayoutView();

        // Pass any configuration change to the drawer toggles
        drawer.drawerToggle.onConfigurationChanged(newConfig);

		drawer.drawerToggle.syncState();

        FolderUi.startTabsHostRun();
    }


    /**
     *  on Back button pressed
     */
    @Override
    public void onBackPressed(){
        System.out.println("MainAct / _onBackPressed");
        doBackKeyEvent();
    }

    void doBackKeyEvent(){
        if (onBackPressedListener != null)
        {
            DB_drawer dbDrawer = new DB_drawer(this);
            int foldersCnt = dbDrawer.getFoldersCount(true);

            if(foldersCnt == 0)
            {
                finish();
                Intent intent  = new Intent(this,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else {
                onBackPressedListener.doBack();
            }
        }
        else
        {
            if((drawer != null) && drawer.isDrawerOpen())
                drawer.closeDrawer();
            else
                super.onBackPressed();
        }

    }


    @Override
    public void onBackStackChanged() {
        int backStackEntryCount = mFragmentManager.getBackStackEntryCount();
        System.out.println("MainAct / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);

        if(backStackEntryCount == 1) // fragment
        {
            System.out.println("MainAct / _onBackStackChanged / fragment");
            initActionBar_home();
        }
        else if(backStackEntryCount == 0) // init
        {
            System.out.println("MainAct / _onBackStackChanged / init");
            onBackPressedListener = null;

            if(mFolder.adapter!=null)
                mFolder.adapter.notifyDataSetChanged();

            configLayoutView();

            drawer.drawerToggle.syncState(); // make sure toggle icon state is correct
        }
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    /**
     * on Activity Result
     */
    AlertDialog.Builder builder;
    AlertDialog alertDlg;
    Handler handler;
    int count;
    String countStr;
    String nextLink;
    String nextTitle;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        System.out.println("MainAct / _onActivityResult ");
        String stringFileName[] = null;

        // mail
        if((requestCode== MailNotes.EMAIL) || (requestCode== MailPagesFragment.EMAIL_PAGES)) {
            if (requestCode == MailNotes.EMAIL)
                stringFileName = MailNotes.mAttachmentFileName;
            else if (requestCode == MailPagesFragment.EMAIL_PAGES)
                stringFileName = MailPagesFragment.mAttachmentFileName;

            Toast.makeText(mAct, R.string.mail_exit, Toast.LENGTH_SHORT).show();

            // note: result code is always 0 (cancel), so it is not used
            new DeleteFileAlarmReceiver(mAct,
                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//					System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
                    stringFileName);
        }
    }

    /***********************************************************************************
     *
     *                                          Menu
     *
     ***********************************************************************************/

    /****************************************************
     *  On Prepare Option menu :
     *  Called whenever we call invalidateOptionsMenu()
     ****************************************************/
    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        System.out.println("MainAct / _onPrepareOptionsMenu");

        if((drawer == null) || (drawer.drawerLayout == null)/* || (!bEULA_accepted)*/)
            return false;

        DB_drawer db_drawer = new DB_drawer(this);
        int foldersCnt = db_drawer.getFoldersCount(true);

        /**
         * Folder group
         */
        // If the navigation drawer is open, hide action items related to the content view
        if(drawer.isDrawerOpen())
        {
            // for landscape: the layout file contains folder menu
            if(Util.isLandscapeOrientation(mAct)) {
                mMenu.setGroupVisible(R.id.group_folders, true);
                // set icon for folder draggable: landscape
                if(MainAct.mPref_show_note_attribute != null)
                {
                    if (MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                            .equalsIgnoreCase("yes"))
                        mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_on_holo_light);
                    else
                        mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_off_holo_light);
                }
            }

//            mMenu.findItem(R.id.DELETE_FOLDERS).setVisible(foldersCnt >0);
//            mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setVisible(foldersCnt >1);

            mMenu.setGroupVisible(R.id.group_pages_and_more, false);
            mMenu.setGroupVisible(R.id.group_notes, false);
        }
        else if(!drawer.isDrawerOpen())
        {
            if(Util.isLandscapeOrientation(mAct))
                mMenu.setGroupVisible(R.id.group_folders, false);

            /**
             * Page group and more
             */
            mMenu.setGroupVisible(R.id.group_pages_and_more, foldersCnt >0);

            if(foldersCnt>0)
            {
                getSupportActionBar().setTitle(mFolderTitle);

                // pages count
                int pgsCnt = FolderUi.getFolder_pagesCount(this,FolderUi.getFocus_folderPos());

                // notes count
                int notesCnt = 0;
                int pageTableId = Pref.getPref_focusView_page_tableId(this);

                if(pageTableId > 0) {
                    DB_page dB_page = new DB_page(this, pageTableId);
                    if (dB_page != null) {
                        try {
                            notesCnt = dB_page.getNotesCount(true);
                        } catch (Exception e) {
                            System.out.println("MainAct / _onPrepareOptionsMenu / dB_page.getNotesCount() error");
                            notesCnt = 0;
                        }
                    }
                }

                // change page color
                mMenu.findItem(R.id.CHANGE_PAGE_COLOR).setVisible(pgsCnt >0);

                // pages order
                mMenu.findItem(R.id.SHIFT_PAGE).setVisible(pgsCnt >1);

                // delete pages
                mMenu.findItem(R.id.DELETE_PAGES).setVisible(pgsCnt >0);

                // note operation
                mMenu.findItem(R.id.note_operation).setVisible( (pgsCnt >0) && (notesCnt>0) );

                // EXPORT TO SD CARD
                mMenu.findItem(R.id.EXPORT_TO_SD_CARD).setVisible(pgsCnt >0);

                // SEND PAGES
                mMenu.findItem(R.id.SEND_PAGES).setVisible(pgsCnt >0);

                /**
                 *  Note group
                 */
                // group of notes
                mMenu.setGroupVisible(R.id.group_notes, pgsCnt > 0);

                // HANDLE CHECKED NOTES
                mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( (pgsCnt >0) && (notesCnt>0) );
            }
            else if(foldersCnt==0)
            {
                /**
                 *  Note group
                 */
                mMenu.setGroupVisible(R.id.group_notes, false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /*************************
     * onCreate Options Menu
     *
     *************************/
    public static MenuItem mSubMenuItemAudio;
    MenuItem playOrStopMusicButton;
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu)
    {
//		System.out.println("MainAct / _onCreateOptionsMenu");
        mMenu = menu;

        // inflate menu
        getMenuInflater().inflate(R.menu.main_menu, menu);

        playOrStopMusicButton = menu.findItem(R.id.PLAY_OR_STOP_MUSIC);

        // enable drag note
        mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
        if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
            menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.drag_note) ;
        else
            menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.drag_note) ;

        // enable show body
        mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
        if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
            menu.findItem(R.id.SHOW_BODY)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.preview_note_body) ;
        else
            menu.findItem(R.id.SHOW_BODY)
                .setIcon(R.drawable.btn_check_off_holo_light)
                .setTitle(R.string.preview_note_body) ;


        return super.onCreateOptionsMenu(menu);
    }

    /******************************
     * on options item selected
     *
     ******************************/
    public static FragmentTransaction mFragmentTransaction;
    public static int mPlaying_pageTableId;
    public static int mPlaying_pagePos;
    public static int mPlaying_folderPos;
    public static int mPlaying_folderTableId;

    static int mMenuUiState;

    public static void setMenuUiState(int mMenuState) {
        mMenuUiState = mMenuState;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //System.out.println("MainAct / _onOptionsItemSelected");
        setMenuUiState(item.getItemId());
        DB_drawer dB_drawer = new DB_drawer(this);
        DB_folder dB_folder = new DB_folder(this, Pref.getPref_focusView_folder_tableId(this));

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Go back: check if Configure fragment now
        if( (item.getItemId() == android.R.id.home ))
        {

            System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / mFragmentManager.getBackStackEntryCount() =" +
            mFragmentManager.getBackStackEntryCount());

            if(mFragmentManager.getBackStackEntryCount() > 0 )
            {
                int foldersCnt = dB_drawer.getFoldersCount(true);
                System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / foldersCnt = " + foldersCnt);

                if(foldersCnt == 0)
                {
                    finish();
                    Intent intent  = new Intent(this,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                {
                    mFragmentManager.popBackStack();

                    initActionBar();

                    mFolderTitle = dB_drawer.getFolderTitle(FolderUi.getFocus_folderPos(),true);
                    setTitle(mFolderTitle);
                    drawer.closeDrawer();
                }
                return true;
            }
        }


        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawer.drawerToggle.onOptionsItemSelected(item))
        {
            System.out.println("MainAct / _onOptionsItemSelected / drawerToggle.onOptionsItemSelected(item) == true ");
            return true;
        }

        switch (item.getItemId())
        {
            case MenuId.ADD_NEW_FOLDER:
                FolderUi.renewFirstAndLast_folderId();
                FolderUi.addNewFolder(this, FolderUi.mLastExist_folderTableId +1, mFolder.getAdapter());
                return true;

            case MenuId.ENABLE_FOLDER_DRAG_AND_DROP:
                if(MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                        .equalsIgnoreCase("yes"))
                {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","no")
                            .apply();
                    DragSortListView listView = (DragSortListView) this.findViewById(R.id.drawer_listview);
                    listView.setDragEnabled(false);
                    Toast.makeText(this,getResources().getString(R.string.drag_folder)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else
                {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","yes")
                            .apply();
                    DragSortListView listView = (DragSortListView) this.findViewById(R.id.drawer_listview);
                    listView.setDragEnabled(true);
                    Toast.makeText(this,getResources().getString(R.string.drag_folder) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                mFolder.getAdapter().notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                return true;

            case MenuId.DELETE_FOLDERS:
                mMenu.setGroupVisible(R.id.group_folders, false);

                if(dB_drawer.getFoldersCount(true)>0)
                {
                    drawer.closeDrawer();
                    mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                    DeleteFolders delFoldersFragment = new DeleteFolders();
                    mFragmentTransaction = mFragmentManager.beginTransaction();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delFoldersFragment).addToBackStack("delete_folders").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
                }
                return true;

            case MenuId.ADD_NEW_NOTE:
                if(Build.VERSION.SDK_INT >= M)//api23
                {
                    // create selection list
                    if(Build.VERSION.SDK_INT >= 30)
                        Add_note_option.createSelection(this, Environment.isExternalStorageManager());
                    else if (Build.VERSION.SDK_INT >= 23) {
                        int permissionWriteExtStorage = ActivityCompat.checkSelfPermission(mAct, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        Add_note_option.createSelection(this, permissionWriteExtStorage == PackageManager.PERMISSION_GRANTED);
                    }
                }
                else
                    Add_note_option.createSelection(this,true);
                return true;

            case MenuId.CHECKED_OPERATION:
                Checked_notes_option op = new Checked_notes_option(this);
                op.open_option_grid(this);
                return true;

            case MenuId.ADD_NEW_PAGE:

                // get current Max page table Id
                int currentMaxPageTableId = 0;
                int pgCnt = FolderUi.getFolder_pagesCount(this,FolderUi.getFocus_folderPos());
                DB_folder db_folder = new DB_folder(this,DB_folder.getFocusFolder_tableId());

                for(int i=0;i< pgCnt;i++)
                {
                    int id = db_folder.getPageTableId(i,true);
                    if(id >currentMaxPageTableId)
                        currentMaxPageTableId = id;
                }

                PageUi.addNewPage(this, currentMaxPageTableId + 1);
                return true;

            case MenuId.CHANGE_PAGE_COLOR:
                PageUi.changePageColor(this);
                return true;

            case MenuId.SHIFT_PAGE:
                PageUi.shiftPage(this);
            return true;

            case MenuId.DELETE_PAGES:
                //hide the menu
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);

                if(dB_folder.getPagesCount(true)>0)
                {
                    DeletePages delPgsFragment = new DeletePages();
                    mFragmentTransaction = mFragmentManager.beginTransaction();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delPgsFragment).addToBackStack("delete_pages").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
            return true;

            case MenuId.ENABLE_NOTE_DRAG_AND_DROP:
                mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
                if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes")) {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE", "no").apply();
                    Toast.makeText(this,getResources().getString(R.string.drag_note)+
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                   Toast.LENGTH_SHORT).show();
                }
                else {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE", "yes").apply();
                    Toast.makeText(this,getResources().getString(R.string.drag_note) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                TabsHost.reloadCurrentPage();
                return true;

            case MenuId.SHOW_BODY:
                mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
                if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes")) {
                    mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY", "no").apply();
                    Toast.makeText(this,getResources().getString(R.string.preview_note_body) +
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                    Toast.LENGTH_SHORT).show();
                }
                else {
                    mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY", "yes").apply();
                    Toast.makeText(this,getResources().getString(R.string.preview_note_body) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                TabsHost.reloadCurrentPage();
                return true;

            // sub menu for backup
            case MenuId.IMPORT_FROM_SD_CARD:
                if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && //API23
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // check permission
                                != PackageManager.PERMISSION_GRANTED))
                {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            Util.PERMISSIONS_REQUEST_STORAGE_IMPORT);
                }
                else {
                    //hide the menu
                    mMenu.setGroupVisible(R.id.group_notes, false);
                    mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                    // replace fragment
                    Import_filesList importFragment = new Import_filesList();
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.content_frame, importFragment, "import").addToBackStack(null).commit();
                }
                return true;

            case MenuId.EXPORT_TO_SD_CARD:
                if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && //API23
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // check permission
                                != PackageManager.PERMISSION_GRANTED))
                {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            Util.PERMISSIONS_REQUEST_STORAGE_EXPORT);
                }
                else {
                    //hide the menu
                    mMenu.setGroupVisible(R.id.group_notes, false);
                    mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                    if (dB_folder.getPagesCount(true) > 0) {
                        Export_toSDCardFragment exportFragment = new Export_toSDCardFragment();
                        transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                        transaction.replace(R.id.content_frame, exportFragment, "export").addToBackStack(null).commit();
                    } else {
                        Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;

            case MenuId.SEND_PAGES:
                if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && //API23
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // check permission
                                != PackageManager.PERMISSION_GRANTED))
                {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            Util.PERMISSIONS_REQUEST_STORAGE);
                }
                else {
                    //hide the menu
                    mMenu.setGroupVisible(R.id.group_notes, false);
                    mMenu.setGroupVisible(R.id.group_pages_and_more, false);

                    if (dB_folder.getPagesCount(true) > 0) {
                        MailPagesFragment mailFragment = new MailPagesFragment();
                        transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                        transaction.replace(R.id.content_frame, mailFragment, "mail").addToBackStack(null).commit();
                    } else {
                        Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;

            case MenuId.CONFIG:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                setTitle(R.string.settings);

                mConfigFragment = new Config();
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;

            case MenuId.ABOUT:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                setTitle(R.string.about_title);

                mAboutFragment = new About();
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mAboutFragment).addToBackStack("about").commit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // configure layout view
    void configLayoutView()
    {
        System.out.println("MainAct / _configLayoutView");

        setContentView(R.layout.drawer);
        initActionBar();

        // new drawer
        drawer = new Drawer(this);
        drawer.initDrawer();

        // new folder
        mFolder = new Folder(this);

        openFolder();
    }

}