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

package com.cw.ListNote.note_edit;

import java.util.Date;

import com.cw.ListNote.db.DB_folder;
import com.cw.ListNote.main.MainAct;
import com.cw.ListNote.R;
import com.cw.ListNote.db.DB_page;
import com.cw.ListNote.tabs.TabsHost;
import com.cw.ListNote.util.ColorSet;
import com.cw.ListNote.util.preferences.Pref;
import com.cw.ListNote.util.Util;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

public class Note_edit_ui {

	private EditText titleEditText;
	private EditText bodyEditText;
	private String oriTitle;
	private String oriBody;

	private Long noteId;
	private Long oriCreatedTime;
	private Long oriMarking;

	boolean bRollBackData;

    private DB_page dB_page;
	private Activity act;
	private int style;
	Note_edit_ui(Activity act, DB_page _db, Long noteId, String strTitle, String strBody, Long createdTime)
    {
    	this.act = act;
    	this.noteId = noteId;
    			
    	oriTitle = strTitle;
	    oriBody = strBody;

	    oriCreatedTime = createdTime;

	    dB_page = _db;//Page.mDb_page;
	    

	    bRollBackData = false;
    }

	void UI_init()
    {
		UI_init_text();

		DB_folder dbFolder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(act));
		style = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

	    final InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

	private void UI_init_text()
	{
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
        DB_folder db = new DB_folder(MainAct.mAct, focusFolder_tableId);
		style = db.getPageStyle(TabsHost.getFocus_tabPos(), true);

		LinearLayout block = (LinearLayout) act.findViewById(R.id.edit_title_block);
		if(block != null)
			block.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		titleEditText = (EditText) act.findViewById(R.id.edit_title);
		bodyEditText = (EditText) act.findViewById(R.id.edit_body);

		//set title color
		titleEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		titleEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		//set body color
		bodyEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		bodyEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
	}

	void deleteNote(Long rowId)
    {
    	System.out.println("Note_edit_ui / _deleteNote");
        // for Add new note (noteId is null first), but decide to cancel
        if(rowId != null)
        	dB_page.deleteNote(rowId,true);
    }
    
    // populate text fields
	void populateFields_text(Long rowId)
	{
		if (rowId != null) {
			// title
			String strTitleEdit = dB_page.getNoteTitle_byId(rowId);
			titleEditText.setText(strTitleEdit);
			titleEditText.setSelection(strTitleEdit.length());

			// body
			String strBodyEdit = dB_page.getNoteBody_byId(rowId);
			bodyEditText.setText(strBodyEdit);
			bodyEditText.setSelection(strBodyEdit.length());
		}
        else
        {
            // renew title
            String strBlank = "";
            titleEditText.setText(strBlank);
            titleEditText.setSelection(strBlank.length());
            titleEditText.requestFocus();

            // renew body
            bodyEditText.setText(strBlank);
            bodyEditText.setSelection(strBlank.length());
        }
	}

    // populate all fields
	void populateFields_all(Long rowId)
    {
    	if (rowId != null) 
    	{
			populateFields_text(rowId);
        }
    }

	private boolean isTitleModified()
    {
    	return !oriTitle.equals(titleEditText.getText().toString());
    }

	private boolean isBodyModified()
    {
    	return !oriBody.equals(bodyEditText.getText().toString());
    }

	boolean isNoteModified()
    {
    	boolean bModified = false;
//		System.out.println("Note_edit_ui / _isNoteModified / isTitleModified() = " + isTitleModified());
//		System.out.println("Note_edit_ui / _isNoteModified / isBodyModified() = " + isBodyModified());
    	if( isTitleModified() ||
    		isBodyModified())
    	{
    		bModified = true;
    	}
    	
    	return bModified;
    }

	Long saveStateInDB(Long rowId,boolean enSaveDb)
	{
    	String title = titleEditText.getText().toString();
        String body = bodyEditText.getText().toString();

        if(enSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( (!Util.isEmptyString(title)) ||
	        		(!Util.isEmptyString(body))    )
	        	{
	        		// insert
	        		System.out.println("Note_edit_ui / _saveStateInDB / insert");
	        		rowId = dB_page.insertNote(title, body, 0, (long) 0);// add new note, get return row Id
	        	}
	        }
	        else // for Edit
	        {
    	        Date now = new Date();
	        	if( !Util.isEmptyString(title) ||
	        		!Util.isEmptyString(body)       )
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_edit_ui / _saveStateInDB / update: roll back");
	        			title = oriTitle;
	        			body = oriBody;
	        			Long time = oriCreatedTime;
	        			dB_page.updateNote(rowId, title,    body, oriMarking, time,true);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_edit_ui / _saveStateInDB / update new");
						System.out.println("--- rowId = " + rowId);
						System.out.println("--- oriMarking = " + oriMarking);

                        long marking;
                        if(null == oriMarking)
                            marking = 0;
                        else
                            marking = oriMarking;

                        boolean isOK;
	        			isOK = dB_page.updateNote(rowId, title, body,marking, now.getTime(),true); // update note
	        			System.out.println("--- isOK = " + isOK);
	        		}
	        	}
	        	else if( Util.isEmptyString(title) &&
	        			 Util.isEmptyString(body)         )
	        	{
	        		// delete
	        		System.out.println("Note_edit_ui / _saveStateInDB / delete");
	        		deleteNote(rowId);
			        rowId = null;
	        	}
	        }
        }

		return rowId;
	}

	public int getCount()
	{
		int noteCount = dB_page.getNotesCount(true);
		return noteCount;
	}
	
}