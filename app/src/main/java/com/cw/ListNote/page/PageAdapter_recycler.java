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

package com.cw.ListNote.page;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cw.ListNote.R;
import com.cw.ListNote.db.DB_folder;
import com.cw.ListNote.db.DB_page;
import com.cw.ListNote.define.Define;
import com.cw.ListNote.main.MainAct;
import com.cw.ListNote.note.Note;
import com.cw.ListNote.note_edit.Note_edit;
import com.cw.ListNote.page.item_touch_helper.ItemTouchHelperAdapter;
import com.cw.ListNote.page.item_touch_helper.ItemTouchHelperViewHolder;
import com.cw.ListNote.page.item_touch_helper.OnStartDragListener;
import com.cw.ListNote.tabs.TabsHost;
import com.cw.ListNote.util.ColorSet;
import com.cw.ListNote.util.Util;
import com.cw.ListNote.util.preferences.Pref;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import static com.cw.ListNote.db.DB_page.KEY_NOTE_BODY;
import static com.cw.ListNote.db.DB_page.KEY_NOTE_CREATED;
import static com.cw.ListNote.db.DB_page.KEY_NOTE_MARKING;
import static com.cw.ListNote.db.DB_page.KEY_NOTE_TITLE;
import static com.cw.ListNote.page.Page_recycler.swapRows;

// Pager adapter
public class PageAdapter_recycler extends RecyclerView.Adapter<PageAdapter_recycler.ViewHolder>
        implements ItemTouchHelperAdapter
{
	private AppCompatActivity mAct;
	Cursor cursor;
	private static int style;
    private DB_folder dbFolder;
	private DB_page mDb_page;
	private int page_pos;
    private final OnStartDragListener mDragStartListener;
	private int page_table_id;
    int title_font_size,body_font_size;

    PageAdapter_recycler(int pagePos,  int pageTableId, OnStartDragListener dragStartListener) {
	    mAct = MainAct.mAct;
	    mDragStartListener = dragStartListener;

        dbFolder = new DB_folder(mAct,Pref.getPref_focusView_folder_tableId(mAct));
	    page_pos = pagePos;
	    page_table_id = pageTableId;
        title_font_size = Pref.getPref_title_font_size(mAct);
        body_font_size = Pref.getPref_body_font_size(mAct);
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        ImageView btnMarking;
        ImageView btnViewNote;
        ImageView btnEditNote;
		TextView rowId;
		TextView textTitle;
		TextView textBody;
		TextView textTime;
        ImageViewCustom btnDrag;

        public ViewHolder(View v) {
            super(v);

            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });

            rowId= (TextView) v.findViewById(R.id.row_id);
            btnMarking = (ImageView) v.findViewById(R.id.btn_marking);
            btnViewNote = (ImageView) v.findViewById(R.id.btn_view_note);
            btnEditNote = (ImageView) v.findViewById(R.id.btn_edit_note);
            btnDrag = (ImageViewCustom) v.findViewById(R.id.btn_drag);
            textTitle = (TextView) v.findViewById(R.id.row_title);
            textBody = (TextView) v.findViewById(R.id.row_body);
            textTime = (TextView) v.findViewById(R.id.row_time);
        }

        public TextView getTextView() {
            return textTitle;
        }

        @Override
        public void onItemSelected() {
//            itemView.setBackgroundColor(Color.LTGRAY);
            ((CardView)itemView).setCardBackgroundColor(MainAct.mAct.getResources().getColor(R.color.button_color));
        }

        @Override
        public void onItemClear() {
            ((CardView)itemView).setCardBackgroundColor(ColorSet.mBG_ColorArray[style]);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.page_view_card, viewGroup, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("Range")
    @Override
    public void onBindViewHolder(ViewHolder holder, final int _position) {

    	int position = holder.getAdapterPosition();
//        System.out.println("PageAdapter_recycler / _onBindViewHolder / position = " + position);

        // style
	    style = dbFolder.getPageStyle(page_pos, true);

        ((CardView)holder.itemView).setCardBackgroundColor(ColorSet.mBG_ColorArray[style]);


        // get DB data
        String strTitle = null;
        String strBody = null;
        Long timeCreated = null;
        int marking = 0;

		SharedPreferences pref_show_note_attribute = MainAct.mAct.getSharedPreferences("show_note_attribute", 0);

	    mDb_page = new DB_page(mAct, page_table_id);
	    mDb_page.open();
	    cursor = mDb_page.mCursor_note;
        if(cursor.moveToPosition(position)) {
            strTitle = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_TITLE));
            strBody = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_BODY));
            marking = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_MARKING));
            timeCreated = cursor.getLong(cursor.getColumnIndex(KEY_NOTE_CREATED));
        }
	    mDb_page.close();

        /**
         *  control block
         */
        // show row Id
        holder.rowId.setText(String.valueOf(position+1));
        holder.rowId.setTextColor(ColorSet.mText_ColorArray[style]);

        // show marking check box
        if(marking == 1)
        {
            holder.btnMarking.setBackgroundResource(style % 2 == 1 ?
                    R.drawable.btn_check_on_holo_light :
                    R.drawable.btn_check_on_holo_dark);
        }
        else
        {
            holder.btnMarking.setBackgroundResource(style % 2 == 1 ?
                    R.drawable.btn_check_off_holo_light :
                    R.drawable.btn_check_off_holo_dark);
        }

        // show drag button
        if(pref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
            holder.btnDrag.setVisibility(View.VISIBLE);
        else
            holder.btnDrag.setVisibility(View.GONE);

		// show text title
		holder.textTitle.setVisibility(View.VISIBLE);
		holder.textTitle.setText(strTitle);
		holder.textTitle.setTextColor(ColorSet.mText_ColorArray[style]);
        holder.textTitle.setTextSize(title_font_size);

		// Show text body
	  	if(pref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
	  	{
			holder.textBody.setText(strBody);

			holder.textBody.setTextColor(ColorSet.mText_ColorArray[style]);
            holder.textBody.setTextSize(body_font_size);

            if(Define.PAGE_VIEW_SHOW_TIME) {
                // time stamp
                holder.textTime.setText(Util.getTimeString(timeCreated));
			    holder.textTime.setTextColor(ColorSet.mText_ColorArray[style]);
                holder.textTime.setTextSize(body_font_size); //same text size as body text
            }
	  	}
	  	else
	  	{
            holder.textBody.setVisibility(View.GONE);

            if(Define.PAGE_VIEW_SHOW_TIME)
                holder.textTime.setVisibility(View.GONE);
	  	}

        setBindViewHolder_listeners(holder,position);
    }


    /**
     * Set bind view holder listeners
     * @param viewHolder
     * @param position
     */
    void setBindViewHolder_listeners(ViewHolder viewHolder, final int position)
    {

//        System.out.println("PageAdapter_recycler / setBindViewHolder_listeners / position = " + position);

        /**
         *  control block
         */
        // on mark note
        viewHolder.btnMarking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                System.out.println("PageAdapter / _getView / btnMarking / _onClick");
                // toggle marking
                toggleNoteMarking(mAct,position);

                //Toggle marking will resume page, so do Store v scroll
                RecyclerView listView = TabsHost.mTabsPagerAdapter.fragmentList.get(TabsHost.getFocus_tabPos()).recyclerView;
                TabsHost.store_listView_vScroll(listView);
                TabsHost.isDoingMarking = true;

                TabsHost.reloadCurrentPage();
                TabsHost.showFooter(MainAct.mAct);

            }
        });

        // on view note
        viewHolder.btnViewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TabsHost.getCurrentPage().mCurrPlayPosition = position;
                DB_page db_page = new DB_page(mAct,TabsHost.getCurrentPageTableId());
                int count = db_page.getNotesCount(true);
                if(position < count)
                {
                    // apply Note class
                    Intent intent;
                    intent = new Intent(mAct, Note.class);
                    intent.putExtra("POSITION", position);
                    mAct.startActivity(intent);
                }
            }
        });

        // on edit note
        viewHolder.btnEditNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DB_page db_page = new DB_page(mAct, TabsHost.getCurrentPageTableId());
                Long rowId = db_page.getNoteId(position,true);

                Intent i = new Intent(mAct, Note_edit.class);
                i.putExtra("list_view_position", position);
                i.putExtra(DB_page.KEY_NOTE_ID, rowId);
                i.putExtra(DB_page.KEY_NOTE_TITLE, db_page.getNoteTitle_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_BODY, db_page.getNoteBody_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_CREATED, db_page.getNoteCreatedTime_byId(rowId));
                mAct.startActivity(i);
            }
        });

        // Start a drag whenever the handle view it touched
        viewHolder.btnDrag.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked())
                {
                    case MotionEvent.ACTION_DOWN:
                        mDragStartListener.onStartDrag(viewHolder);
                        System.out.println("PageAdapter_recycler / onTouch / ACTION_DOWN");
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        return true;
                }
                return false;
            }


        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
	    mDb_page = new DB_page(mAct, page_table_id);
	    return  mDb_page.getNotesCount(true);
    }

    // toggle mark of note
    public static int toggleNoteMarking(AppCompatActivity mAct, int position)
    {
        int marking = 0;
		DB_page db_page = new DB_page(mAct,TabsHost.getCurrentPageTableId());
        db_page.open();
        int count = db_page.getNotesCount(false);
        if(position >= count) //end of list
        {
            db_page.close();
            return marking;
        }

        String strNote = db_page.getNoteTitle(position,false);
        String strNoteBody = db_page.getNoteBody(position,false);
        Long idNote =  db_page.getNoteId(position,false);

        // toggle the marking
        if(db_page.getNoteMarking(position,false) == 0)
        {
            db_page.updateNote(idNote, strNote, strNoteBody, 1, 0, false);
            marking = 1;
        }
        else
        {
            db_page.updateNote(idNote, strNote, strNoteBody, 0, 0, false);
            marking = 0;
        }
        db_page.close();

        System.out.println("PageAdapter_recycler / _toggleNoteMarking / position = " + position + ", marking = " + db_page.getNoteMarking(position,true));
        return  marking;
    }

    @Override
    public void onItemDismiss(int position) {
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPos, int toPos) {
//        System.out.println("PageAdapter_recycler / _onItemMove / fromPos = " +
//                        fromPos + ", toPos = " + toPos);

        notifyItemMoved(fromPos, toPos);

        int oriStartPos = fromPos;
        int oriEndPos = toPos;

        mDb_page = new DB_page(mAct, TabsHost.getCurrentPageTableId());
        if(fromPos >= mDb_page.getNotesCount(true)) // avoid footer error
            return false;

        //reorder data base storage
        int loop = Math.abs(fromPos-toPos);
        for(int i=0;i< loop;i++)
        {
            swapRows(mDb_page, fromPos,toPos);
            if((fromPos-toPos) >0)
                toPos++;
            else
                toPos--;
        }

        // update footer
        TabsHost.showFooter(mAct);
        return true;
    }

    @Override
    public void onItemMoved(RecyclerView.ViewHolder sourceViewHolder, int fromPos, RecyclerView.ViewHolder targetViewHolder, int toPos) {
        System.out.println("PageAdapter_recycler / _onItemMoved");
        ((TextView)sourceViewHolder.itemView.findViewById(R.id.row_id)).setText(String.valueOf(toPos+1));
        ((TextView)targetViewHolder.itemView.findViewById(R.id.row_id)).setText(String.valueOf(fromPos+1));

        setBindViewHolder_listeners((ViewHolder)sourceViewHolder,toPos);
        setBindViewHolder_listeners((ViewHolder)targetViewHolder,fromPos);
    }

}
