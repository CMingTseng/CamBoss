package com.tutk.P2PCam264.DELUX.SearchEvent;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.actionbarsherlock.app.SherlockActivity;
import com.tutk.IOTC.AVIOCTRLDEFs;
import com.tutk.IOTC.Camera;
import com.tutk.IOTC.IRegisterIOTCListener;
import com.tutk.P2PCam264.DELUX.Activity.qr_codeActivity;
import com.tutk.P2PCam264.DELUX.MultiView.Activity.MultiViewActivity;
import com.tutk.P2PCam264.DELUX.Structure.DeviceInfo;
import com.tutk.P2PCam264.MyCamera;
import com.tutk.P2PCam264.R;
import com.tutk.P2PCam264.ui.Custom_popupWindow;
import com.tutk.P2PCam264.util.DatabaseManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class SearchEventActivity extends SherlockActivity {

    private static final int Build_VERSION_CODES_ICE_CREAM_SANDWICH = 14;

    private static final int SEARCH_WITHIN_AN_HOUR = 0;
    private static final int SEARCH_WITHIN_HALF_A_DAY = 1;
    private static final int SEARCH_WITHIN_A_DAY = 2;
    private static final int SEARCH_WITHIN_A_WEEK = 3;

    private Calendar mStartSearchCalendar;
    private Calendar mStopSearchCalendar;
    private int mSearchEventType;

    private String mDevUID;

    private List<CachedHistory> list = Collections.synchronizedList(new ArrayList<CachedHistory>());
    private SearchListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.titlebar);
        TextView tv = (TextView) this.findViewById(R.id.bar_text);
        tv.setText(getText(R.string.app_name));

        setContentView(R.layout.search_event);

        if (Build.VERSION.SDK_INT < Build_VERSION_CODES_ICE_CREAM_SANDWICH) {
            BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped);
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);
        }

        Bundle bundle = this.getIntent().getExtras();
        mDevUID = bundle.getString("dev_uid");

        adapter = new SearchListAdapter(this);
        ListView listView = (ListView) findViewById(R.id.lstEventSearch);

        TextView txtView;

        View searchWithinAnHour = getLayoutInflater().inflate(R.layout.search_event_predefined_item, null);
        txtView = (TextView) searchWithinAnHour.findViewById(R.id.txtSearchPredefined);
        txtView.setText(getText(R.string.tips_search_within_an_hour));
        listView.addHeaderView(searchWithinAnHour);

        View searchWithinHalfDay = getLayoutInflater().inflate(R.layout.search_event_predefined_item, null);
        txtView = (TextView) searchWithinHalfDay.findViewById(R.id.txtSearchPredefined);
        txtView.setText(getText(R.string.tips_search_within_half_a_day));
        listView.addHeaderView(searchWithinHalfDay);

        View searchWithinADay = getLayoutInflater().inflate(R.layout.search_event_predefined_item, null);
        txtView = (TextView) searchWithinADay.findViewById(R.id.txtSearchPredefined);
        txtView.setText(getText(R.string.tips_search_within_a_day));
        listView.addHeaderView(searchWithinADay);

        View searchWithinAWeek = getLayoutInflater().inflate(R.layout.search_event_predefined_item, null);
        txtView = (TextView) searchWithinAWeek.findViewById(R.id.txtSearchPredefined);
        txtView.setText(getText(R.string.tips_search_within_a_week));
        listView.addHeaderView(searchWithinAWeek);

        View searchCustom = getLayoutInflater().inflate(R.layout.search_event_predefined_item, null);
        txtView = (TextView) searchCustom.findViewById(R.id.txtSearchPredefined);
        txtView.setText(getText(R.string.tips_search_custom));
        listView.addHeaderView(searchCustom);

        initCachedHistoryList();

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(listViewOnItemClickListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // FlurryAgent.onStartSession(this, "Q1SDXDZQ21BQMVUVJ16W");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // FlurryAgent.onEndSession(this);
    }

    private AdapterView.OnItemClickListener listViewOnItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {

            Bundle extras = new Bundle();
            Intent intent = new Intent();

            Calendar calStart = Calendar.getInstance(TimeZone.getTimeZone("gmt"));
            Calendar calStop = Calendar.getInstance(TimeZone.getTimeZone("gmt"));

            if (position < 4) {

                switch (position) {
                    case SEARCH_WITHIN_AN_HOUR:
                        calStart.add(Calendar.HOUR, -1);
                        break;

                    case SEARCH_WITHIN_HALF_A_DAY:
                        calStart.add(Calendar.HOUR_OF_DAY, -12);
                        break;

                    case SEARCH_WITHIN_A_DAY:
                        calStart.add(Calendar.DAY_OF_YEAR, -1);
                        break;

                    case SEARCH_WITHIN_A_WEEK:
                        calStart.add(Calendar.DAY_OF_YEAR, -7);
                        break;
                }

                extras.putInt("event_type", 0);
                extras.putLong("start_time", calStart.getTimeInMillis());
                extras.putLong("stop_time", calStop.getTimeInMillis());

                intent.putExtras(extras);
                setResult(RESULT_OK, intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);

            } else if (position == 4) {
                showCustomSearch();
            } else {

                int idx = position - 5;
                CachedHistory cachedHistory = list.get(idx);

                calStart.setTimeInMillis(cachedHistory.StartTime);
                calStop.setTimeInMillis(cachedHistory.StopTime);

                extras.putInt("event_type", cachedHistory.EventType);
                extras.putLong("start_time", calStart.getTimeInMillis());
                extras.putLong("stop_time", calStop.getTimeInMillis());

                intent.putExtras(extras);
                setResult(RESULT_OK, intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            }
        }
    };

    private void initCachedHistoryList() {

        DatabaseManager manager = new DatabaseManager(this);
        SQLiteDatabase db = manager.getReadableDatabase();
        Cursor cursor = db.query(DatabaseManager.TABLE_SEARCH_HISTORY, new String[]{"_id", "dev_uid", "search_event_type", "search_start_time",
                "search_stop_time"}, "dev_uid = '" + mDevUID + "'", null, null, null, "_id DESC LIMIT 10");

        while (cursor.moveToNext()) {

            // long db_id = cursor.getLong(0);
            // String dev_uid = cursor.getString(1);
            int event_type = cursor.getInt(2);
            long start_time = cursor.getLong(3);
            long stop_time = cursor.getLong(4);

            CachedHistory history = new CachedHistory(event_type, start_time, stop_time);
            list.add(history);
        }

        cursor.close();
        db.close();
    }

    private void showCustomSearch() {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.HoloAlertDialog));
        final AlertDialog dlg = builder.create();
        dlg.setTitle(getText(R.string.dialog_EventSearch));
        dlg.setIcon(android.R.drawable.ic_dialog_info);

        LayoutInflater inflater = dlg.getLayoutInflater();
        View view = inflater.inflate(R.layout.search_event_custom, null);
        dlg.setView(view);

        final Spinner spinEventType = (Spinner) view.findViewById(R.id.spinEventType);
        final Button btnStartDate = (Button) view.findViewById(R.id.btnStartDate);
        final Button btnStartTime = (Button) view.findViewById(R.id.btnStartTime);
        final Button btnStopDate = (Button) view.findViewById(R.id.btnStopDate);
        final Button btnStopTime = (Button) view.findViewById(R.id.btnStopTime);
        Button btnOK = (Button) view.findViewById(R.id.btnOK);
        Button btnCancel = (Button) view.findViewById(R.id.btnCancel);

        // set button
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        SearchEventActivity.this.mStartSearchCalendar = Calendar.getInstance();
        SearchEventActivity.this.mStartSearchCalendar.set(Calendar.SECOND, 0);
        SearchEventActivity.this.mStopSearchCalendar = Calendar.getInstance();
        SearchEventActivity.this.mStopSearchCalendar.set(Calendar.SECOND, 0);

        btnStartDate.setText(dateFormat.format(SearchEventActivity.this.mStartSearchCalendar.getTime()));
        btnStartTime.setText(timeFormat.format(SearchEventActivity.this.mStartSearchCalendar.getTime()));
        btnStopDate.setText(dateFormat.format(SearchEventActivity.this.mStopSearchCalendar.getTime()));
        btnStopTime.setText(timeFormat.format(SearchEventActivity.this.mStopSearchCalendar.getTime()));

        // set spinner adapter & listener
        ArrayAdapter<CharSequence> adapter = ArrayAdapter
                .createFromResource(SearchEventActivity.this, R.array.event_type, R.layout.search_event_myspinner);
        adapter.setDropDownViewResource(R.layout.search_event_myspinner);


        spinEventType.setAdapter(adapter);
        spinEventType.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SearchEventActivity.this.mSearchEventType = position;

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        final DatePickerDialog.OnDateSetListener startDateOnDateSetListener = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                SearchEventActivity.this.mStartSearchCalendar.set(year, monthOfYear, dayOfMonth,
                        SearchEventActivity.this.mStartSearchCalendar.get(Calendar.HOUR_OF_DAY),
                        SearchEventActivity.this.mStartSearchCalendar.get(Calendar.MINUTE), 0);

                btnStartDate.setText(dateFormat.format(SearchEventActivity.this.mStartSearchCalendar.getTime()));

                // todo:
                // if start time > stop time , then stop time = start time
                //

                if (SearchEventActivity.this.mStartSearchCalendar.after(SearchEventActivity.this.mStopSearchCalendar)) {

                    SearchEventActivity.this.mStopSearchCalendar.setTimeInMillis(SearchEventActivity.this.mStartSearchCalendar.getTimeInMillis());
                    btnStopDate.setText(dateFormat.format(SearchEventActivity.this.mStopSearchCalendar.getTime()));
                    btnStopTime.setText(timeFormat.format(SearchEventActivity.this.mStopSearchCalendar.getTime()));
                }
            }
        };

        final DatePickerDialog.OnDateSetListener stopDateOnDateSetListener = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                // todo:
                // let tmp = after set stop time
                // if tmp time < start time, do nothing.
                //
                Calendar tmp = Calendar.getInstance();
                tmp.set(year, monthOfYear, dayOfMonth, SearchEventActivity.this.mStopSearchCalendar.get(Calendar.HOUR_OF_DAY),
                        SearchEventActivity.this.mStopSearchCalendar.get(Calendar.MINUTE), 0);

                if (tmp.after(SearchEventActivity.this.mStartSearchCalendar) || tmp.equals(SearchEventActivity.this.mStartSearchCalendar)) {

                    SearchEventActivity.this.mStopSearchCalendar.set(year, monthOfYear, dayOfMonth,
                            SearchEventActivity.this.mStopSearchCalendar.get(Calendar.HOUR_OF_DAY),
                            SearchEventActivity.this.mStopSearchCalendar.get(Calendar.MINUTE), 0);

                    btnStopDate.setText(dateFormat.format(SearchEventActivity.this.mStopSearchCalendar.getTime()));

                }
            }
        };

        final TimePickerDialog.OnTimeSetListener startTimeOnTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                SearchEventActivity.this.mStartSearchCalendar.set(SearchEventActivity.this.mStartSearchCalendar.get(Calendar.YEAR),
                        SearchEventActivity.this.mStartSearchCalendar.get(Calendar.MONTH),
                        SearchEventActivity.this.mStartSearchCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);

                btnStartTime.setText(timeFormat.format(SearchEventActivity.this.mStartSearchCalendar.getTime()));

                // todo:
                // if start time > stop time , then stop time = start time
                //
                if (SearchEventActivity.this.mStartSearchCalendar.after(SearchEventActivity.this.mStopSearchCalendar)) {

                    SearchEventActivity.this.mStopSearchCalendar.setTimeInMillis(SearchEventActivity.this.mStartSearchCalendar.getTimeInMillis());
                    btnStopTime.setText(timeFormat.format(SearchEventActivity.this.mStopSearchCalendar.getTime()));
                }
            }
        };

        final TimePickerDialog.OnTimeSetListener stopTimeOnTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                // todo:
                // let tmp = after set stop time
                // if tmp time < start time, do nothing.
                //
                Calendar tmp = Calendar.getInstance();
                tmp.set(SearchEventActivity.this.mStopSearchCalendar.get(Calendar.YEAR), SearchEventActivity.this.mStopSearchCalendar.get(Calendar.MONTH),
                        SearchEventActivity.this.mStopSearchCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute, 0);

                if (tmp.after(SearchEventActivity.this.mStartSearchCalendar) || tmp.equals(SearchEventActivity.this.mStartSearchCalendar)) {

                    SearchEventActivity.this.mStopSearchCalendar.set(SearchEventActivity.this.mStopSearchCalendar.get(Calendar.YEAR),
                            SearchEventActivity.this.mStopSearchCalendar.get(Calendar.MONTH),
                            SearchEventActivity.this.mStopSearchCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);

                    btnStopTime.setText(timeFormat.format(SearchEventActivity.this.mStopSearchCalendar.getTime()));
                }
            }
        };

        btnStartDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Calendar cal = Calendar.getInstance();

                new DatePickerDialog(SearchEventActivity.this, startDateOnDateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal
                        .get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        btnStartTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Calendar cal = Calendar.getInstance();

                new TimePickerDialog(SearchEventActivity.this, startTimeOnTimeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false)
                        .show();
            }
        });

        btnStopDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Calendar cal = Calendar.getInstance();

                new DatePickerDialog(SearchEventActivity.this, stopDateOnDateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal
                        .get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        btnStopTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Calendar cal = Calendar.getInstance();

                new TimePickerDialog(SearchEventActivity.this, stopTimeOnTimeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false)
                        .show();
            }
        });

        btnOK.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//20130725 chun 
//				DatabaseManager manager = new DatabaseManager(SearchEventActivity.this);
//				manager.addSearchHistory(SearchEventActivity.this.mDevUID, SearchEventActivity.this.mSearchEventType, mStartSearchCalendar.getTimeInMillis(),
//						mStopSearchCalendar.getTimeInMillis());

                Bundle extras = new Bundle();
                extras.putInt("event_type", SearchEventActivity.this.mSearchEventType);
                extras.putLong("start_time", mStartSearchCalendar.getTimeInMillis());
                extras.putLong("stop_time", mStopSearchCalendar.getTimeInMillis());

                Intent intent = new Intent();
                intent.putExtras(extras);
                setResult(RESULT_OK, intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);

                dlg.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                dlg.dismiss();
            }
        });

        dlg.show();
    }

    private class CachedHistory {

        public int EventType;
        public long StartTime;
        public long StopTime;

        public CachedHistory(int type, long start_time, long stop_time) {
            EventType = type;
            StartTime = start_time;
            StopTime = stop_time;
        }
    }

    private class SearchListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public SearchListAdapter(Context context) {

            this.mInflater = LayoutInflater.from(context);
        }

        public int getCount() {

            return list.size();
        }

        public Object getItem(int position) {

            return list.get(position);
        }

        public long getItemId(int position) {

            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            final CachedHistory cachedHistory = (CachedHistory) getItem(position);
            ViewHolder holder = null;

            if (convertView == null) {

                convertView = mInflater.inflate(R.layout.search_event_history_item, null);

                holder = new ViewHolder();
                holder.txtType = (TextView) convertView.findViewById(R.id.txtEventType);
                holder.txtStartTime = (TextView) convertView.findViewById(R.id.txtStartTime);
                holder.txtStopTime = (TextView) convertView.findViewById(R.id.txtStopTime);

                convertView.setTag(holder);

            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            if (cachedHistory != null) {

                Calendar start = Calendar.getInstance(TimeZone.getTimeZone("gmt"));
                Calendar stop = Calendar.getInstance(TimeZone.getTimeZone("gmt"));

                start.setTimeInMillis(cachedHistory.StartTime);
                stop.setTimeInMillis(cachedHistory.StopTime);

                SimpleDateFormat format = new SimpleDateFormat();
                format.setTimeZone(TimeZone.getDefault());

                holder.txtType.setText(MultiViewActivity.getEventType(SearchEventActivity.this, cachedHistory.EventType, true));
                holder.txtStartTime.setText(format.format(start.getTime()));
                holder.txtStopTime.setText(format.format(stop.getTime()));
            }

            return convertView;
        }

        private final class ViewHolder {
            public TextView txtType;
            public TextView txtStartTime;
            public TextView txtStopTime;
        }

    }

    public static class DeviceListActivity extends SherlockActivity implements IRegisterIOTCListener, View.OnClickListener,
            Custom_popupWindow.On_PopupWindow_click_Listener {

        private final String TAG = "DeviceListActivity";
        private static final int Build_VERSION_CODES_ICE_CREAM_SANDWICH = 14;
        public static final int CAMERA_MAX_LIMITS = 4;

        private DeviceListActivity mActivity;
        private DeviceListAdapter mAdapter;

        public static final int REQUEST_CODE_CAMERA_ADD = 0;
        public static final int REQUEST_CODE_CLOUDCAMERA_ADD = 1;
        public static final int Result_CLOUD_CAMERA_Cancel = 6;
        public static final int Result_CLOUD_CAMERA_OK = 7;
        public static int nShowMessageCount = 0;

        private int mMonitorIndex = -1;
        private String OriginallyUID;
        private int OriginallyChannelIndex;

        private TextView mNullDeviceTxt;

        private ListView mCameraList;
        private boolean mIsRemovable = false;
        private boolean mScroll = false;

        private View addDeviceView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mActivity = this;

            ActionBar actionBar = getActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            TextView tv = (TextView) this.findViewById(R.id.bar_text);
            tv.setText(getText(R.string.txt_Devices_List));

            if (Build.VERSION.SDK_INT < Build_VERSION_CODES_ICE_CREAM_SANDWICH) {
                BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped);
                bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
                getSupportActionBar().setBackgroundDrawable(bg);
            }

            Bundle bundle = this.getIntent().getExtras();
            mMonitorIndex = bundle.getInt("MonitorIndex");
            OriginallyUID = bundle.getString("OriginallyUID");
            OriginallyChannelIndex = bundle.getInt("OriginallyChannelIndex");
            setupView();

            if (MultiViewActivity.DeviceList.size() == 0) {
                Intent intent = new Intent();
                intent.setClass(DeviceListActivity.this, qr_codeActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CAMERA_ADD);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            quit();
        }

        @Override
        protected void onStart() {
            super.onStart();
        }

        @Override
        protected void onStop() {
            super.onStop();
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {

            if (keyCode == KeyEvent.KEYCODE_BACK) {

                quit();
                return false;

            }

            return super.onKeyDown(keyCode, event);
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);

            Configuration cfg = getResources().getConfiguration();

            if (cfg.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                System.out.println("ORIENTATION_LANDSCAPE");

            } else if (cfg.orientation == Configuration.ORIENTATION_PORTRAIT) {
                System.out.println("ORIENTATION_PORTRAIT");
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_CODE_CAMERA_ADD || requestCode == REQUEST_CODE_CLOUDCAMERA_ADD) {

                switch (resultCode) {
                    case RESULT_OK:

                        Bundle extras = data.getExtras();
                        long db_id = extras.getLong("db_id");
                        String dev_nickname = extras.getString("dev_nickname");
                        String dev_uid = extras.getString("dev_uid");
                        String view_acc = extras.getString("view_acc");
                        String view_pwd = extras.getString("view_pwd");
                        int event_notification = 3;
                        int channel = extras.getInt("camera_channel");

                        MyCamera camera = new MyCamera(dev_nickname, dev_uid, view_acc, view_pwd);
                        DeviceInfo dev = new DeviceInfo(db_id, camera.getUUID(), dev_nickname, dev_uid, view_acc, view_pwd, "", event_notification, channel,
                                null, null, null, null, null);
                        MultiViewActivity.DeviceList.add(dev);

                        camera.registerIOTCListener(this);
                        camera.registerIOTCListener(MultiViewActivity.getMultiViewActivityIRegisterIOTCListener());
                        camera.connect(dev_uid);
                        camera.start(MyCamera.DEFAULT_AV_CHANNEL, view_acc, view_pwd);
                        camera.sendIOCtrl(MyCamera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVINFO_REQ,
                                AVIOCTRLDEFs.SMsgAVIoctrlDeviceInfoReq.parseContent());
                        camera.sendIOCtrl(MyCamera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSUPPORTSTREAM_REQ,
                                AVIOCTRLDEFs.SMsgAVIoctrlGetSupportStreamReq.parseContent());
                        camera.sendIOCtrl(MyCamera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_REQ,
                                AVIOCTRLDEFs.SMsgAVIoctrlGetAudioOutFormatReq.parseContent());
                        camera.sendIOCtrl(MyCamera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_TIMEZONE_REQ,
                                AVIOCTRLDEFs.SMsgAVIoctrlTimeZone.parseContent());

                        camera.LastAudioMode = 1;

                        MultiViewActivity.CameraList.add(camera);

                        mAdapter.notifyDataSetChanged();
                        verifyCameraLimit();

                        Bundle NEWextras = new Bundle();
                        NEWextras.putString("dev_uid", dev.UID);
                        NEWextras.putString("dev_uuid", dev.UUID);
                        NEWextras.putString("dev_nickname", dev.NickName);
                        NEWextras.putString("conn_status", dev.Status);
                        NEWextras.putString("view_acc", dev.View_Account);
                        NEWextras.putString("view_pwd", dev.View_Password);
                        NEWextras.putString("OriginallyUID", OriginallyUID);
                        NEWextras.putInt("OriginallyChannelIndex", OriginallyChannelIndex);
                        // ADD DEVICE CLANNEL IS ALWAYS 0
                        NEWextras.putInt("camera_channel", MyCamera.DEFAULT_AV_CHANNEL);
                        NEWextras.putInt("MonitorIndex", mMonitorIndex);
                        Intent Intent = new Intent();
                        Intent.putExtras(NEWextras);
                        DeviceListActivity.this.setResult(RESULT_OK, Intent);
                        DeviceListActivity.this.finish();

                        break;

                    case RESULT_CANCELED:
                        if (MultiViewActivity.DeviceList.size() == 0)
                            quit();
                        break;
                }
            }
        }

        @Override
        protected void onPause() {
            super.onPause();

        }

        @Override
        protected void onResume() {
            super.onResume();

        }

        private void quit() {

            finish();// IOTC
            overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);

            for (MyCamera camera : MultiViewActivity.CameraList) {
                // camera.stop(MyCamera.DEFAULT_AV_CHANNEL);
                camera.unregisterIOTCListener(this);
            }

        }

        private void setupView() {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.devicelistactivty);
            mNullDeviceTxt = (TextView) findViewById(R.id.txt_null_device);
            if (MultiViewActivity.DeviceList.size() == 0) {
                mNullDeviceTxt.setVisibility(View.VISIBLE);
            }

            mCameraList = (ListView) findViewById(R.id.lstCameraList);
            addDeviceView = getLayoutInflater().inflate(R.layout.add_device_row, null);
            mAdapter = new DeviceListAdapter(this);
            mCameraList.addFooterView(addDeviceView);
            mCameraList.setAdapter(mAdapter);
            mCameraList.setOnItemClickListener(listViewOnItemClickListener);

            verifyCameraLimit();
        }

        private AdapterView.OnItemClickListener listViewOnItemClickListener = new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                if (position < MultiViewActivity.DeviceList.size()) {
                    MyCamera cam = MultiViewActivity.CameraList.get(position);
                    if (cam.isSessionConnected() && cam.getMultiStreamSupported(0) && cam.getSupportedStream().length > 1) {
                        PopupWindow popupWindow = null;
                        ViewGroup layout = null;
                        int width = 0;
                        int height = 0;
                        DisplayMetrics metrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(metrics);
                        if (metrics.heightPixels < 900 && position == (MultiViewActivity.DeviceList.size() - 1) && cam.getSupportedStream().length > 5) {
                            mCameraList.scrollTo(0, 100);
                            mScroll = true;
                        }

                        layout = (LinearLayout) LayoutInflater.from(DeviceListActivity.this).inflate(R.layout.popup_devices_ch, null);
                        popupWindow = Custom_popupWindow.Menu_PopupWindow_newInstance(DeviceListActivity.this, layout, DeviceListActivity.this,
                                Custom_popupWindow.DEVICES_CH, cam.getSupportedStream().length, position);
                        width = (int) (metrics.widthPixels - (popupWindow.getWidth() + getResources().getDimensionPixelSize(R.dimen.bubble_add_offset_w)));
                        height = getResources().getDimensionPixelSize(R.dimen.bubble_add_offset_h);
                        popupWindow.showAsDropDown(v, width, -height);
                        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                            @Override
                            public void onDismiss() {
                                if (mScroll)
                                    mCameraList.scrollTo(0, 0);

                                mScroll = false;
                            }
                        });
                    } else {
                        Bundle extras = new Bundle();
                        extras.putString("dev_uid", MultiViewActivity.DeviceList.get(position).UID);
                        extras.putString("dev_uuid", MultiViewActivity.DeviceList.get(position).UUID);
                        extras.putString("dev_nickname", MultiViewActivity.DeviceList.get(position).NickName);
                        extras.putString("conn_status", MultiViewActivity.DeviceList.get(position).Status);
                        extras.putString("view_acc", MultiViewActivity.DeviceList.get(position).View_Account);
                        extras.putString("view_pwd", MultiViewActivity.DeviceList.get(position).View_Password);
                        extras.putString("OriginallyUID", OriginallyUID);
                        extras.putInt("OriginallyChannelIndex", OriginallyChannelIndex);
                        // add device always channel0
                        extras.putInt("camera_channel", MyCamera.DEFAULT_AV_CHANNEL);
                        extras.putInt("MonitorIndex", mMonitorIndex);
                        Intent Intent = new Intent();
                        Intent.putExtras(extras);
                        DeviceListActivity.this.setResult(RESULT_OK, Intent);
                        DeviceListActivity.this.finish();
                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    }
                } else {
                    if (MultiViewActivity.CameraList.size() < CAMERA_MAX_LIMITS) {

                        Intent intent = new Intent();
                        intent.setClass(DeviceListActivity.this, qr_codeActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_CAMERA_ADD);
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    }
                }
            }
        };

        private void verifyCameraLimit() {

            if (MultiViewActivity.DeviceList.size() < CAMERA_MAX_LIMITS) {

                if (mCameraList.getFooterViewsCount() == 0) {
                    mCameraList.addFooterView(addDeviceView);
                    mAdapter.notifyDataSetChanged();
                }

            } else {

                if (mCameraList.getFooterViewsCount() > 0) {
                    mCameraList.removeFooterView(addDeviceView);
                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        public static void showAlert(Context context, CharSequence title, CharSequence message, CharSequence btnTitle) {

            AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context);
            dlgBuilder.setIcon(android.R.drawable.ic_dialog_alert);
            dlgBuilder.setTitle(title);
            dlgBuilder.setMessage(message);
            dlgBuilder.setPositiveButton(btnTitle, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
        }

        @Override
        public void receiveFrameData(final Camera camera, int sessionChannel, Bitmap bmp) {

        }

        @Override
        public void receiveFrameInfo(final Camera camera, int sessionChannel, long bitRate, int frameRate, int onlineNm, int frameCount,
                                     int incompleteFrameCount) {

        }

        @Override
        public void receiveSessionInfo(final Camera camera, int resultCode) {

            Bundle bundle = new Bundle();
            bundle.putString("requestDevice", ((MyCamera) camera).getUUID());

            Message msg = handler.obtainMessage();
            msg.what = resultCode;
            msg.setData(bundle);
            handler.sendMessage(msg);
        }

        @Override
        public void receiveChannelInfo(final Camera camera, int sessionChannel, int resultCode) {

            Bundle bundle = new Bundle();
            bundle.putString("requestDevice", ((MyCamera) camera).getUUID());
            bundle.putInt("sessionChannel", sessionChannel);

            Message msg = handler.obtainMessage();
            msg.what = resultCode;
            msg.setData(bundle);
            handler.sendMessage(msg);
        }

        @Override
        public void receiveIOCtrlData(final Camera camera, int sessionChannel, int avIOCtrlMsgType, byte[] data) {

            Bundle bundle = new Bundle();
            bundle.putString("requestDevice", ((MyCamera) camera).getUUID());
            bundle.putInt("sessionChannel", sessionChannel);
            bundle.putByteArray("data", data);

            Message msg = handler.obtainMessage();
            msg.what = avIOCtrlMsgType;
            msg.setData(bundle);
            handler.sendMessage(msg);
        }

        private Handler handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {

                mAdapter.notifyDataSetChanged();
                super.handleMessage(msg);
            }
        };

        private class DeviceListAdapter extends BaseAdapter {

            private LayoutInflater mInflater;

            public DeviceListAdapter(Context context) {

                this.mInflater = LayoutInflater.from(context);
            }

            public int getCount() {

                return MultiViewActivity.DeviceList.size();
            }

            public Object getItem(int position) {

                return MultiViewActivity.DeviceList.get(position);
            }

            public long getItemId(int position) {

                return position;
            }

            public View getView(final int position, View convertView, ViewGroup parent) {

                final DeviceInfo dev = MultiViewActivity.DeviceList.get(position);
                final MyCamera cam = MultiViewActivity.CameraList.get(position);

                if (dev == null || cam == null)
                    return null;

                ViewHolder holder = null;

                if (convertView == null) {

                    convertView = mInflater.inflate(R.layout.device_list, null);

                    holder = new ViewHolder();
                    holder.img = (ImageView) convertView.findViewById(R.id.img);
                    holder.title = (TextView) convertView.findViewById(R.id.title);
                    holder.info = (TextView) convertView.findViewById(R.id.info);
                    holder.status = (TextView) convertView.findViewById(R.id.status);
                    holder.eventLayout = (FrameLayout) convertView.findViewById(R.id.eventLayout);
                    holder.GCM_Prompt = (TextView) convertView.findViewById(R.id.GCM_Prompt);
                    holder.more = (ImageView) convertView.findViewById(R.id.more);
                    holder.layoutCH = (RelativeLayout) convertView.findViewById(R.id.layoutCH);
                    holder.txtCH = (TextView) convertView.findViewById(R.id.txtCH);
                    holder.imgCH = (ImageView) convertView.findViewById(R.id.imgCH);
                    convertView.setTag(holder);

                } else {

                    holder = (ViewHolder) convertView.getTag();
                }

                if (holder != null) {

                    holder.eventLayout.setVisibility(View.GONE);
                    holder.img.setImageBitmap(dev.Snapshot);
                    holder.title.setText(dev.NickName);
                    holder.info.setText(dev.UID);
                    holder.layoutCH.setVisibility(View.VISIBLE);
                    if (cam.isSessionConnected() && cam.getMultiStreamSupported(0) && cam.getSupportedStream().length > 1) {
                        holder.txtCH.setText("" + cam.getSupportedStream().length);
                        holder.txtCH.setTextColor(Color.BLACK);
                        holder.imgCH.setBackgroundResource(R.drawable.btn_camera_s_n);
                    } else {
                        holder.txtCH.setText("1");
                        holder.txtCH.setTextColor(Color.WHITE);
                        holder.imgCH.setBackgroundResource(R.drawable.btn_camera_s_h);
                    }
                    if (dev.n_gcm_count == 0) {
                        holder.GCM_Prompt.setVisibility(View.GONE);
                    } else {
                        holder.GCM_Prompt.setVisibility(View.VISIBLE);
                        holder.GCM_Prompt.setText(Integer.toString(dev.n_gcm_count));
                    }
                    if (nShowMessageCount == 0) {
                        holder.status.setText(dev.Status);
                    } else {
                        holder.status.setText(dev.Status + " " + cam.gettempAvIndex());
                    }
                    if (!cam.isSessionConnected()) {
                        convertView.setBackgroundResource(R.color.bg_gray);
                        holder.title.setTextColor(getResources().getColor(R.color.txt_gray));
                        holder.info.setTextColor(getResources().getColor(R.color.txt_gray));
                        holder.status.setTextColor(getResources().getColor(R.color.txt_gray));
                    } else {
                        convertView.setBackgroundResource(R.color.item_black);
                        holder.title.setTextColor(Color.WHITE);
                        holder.info.setTextColor(Color.WHITE);
                        holder.status.setTextColor(Color.WHITE);
                    }

                }

                return convertView;

            }

            public final class ViewHolder {
                public ImageView img;
                public TextView title;
                public TextView info;
                public TextView status;
                public TextView GCM_Prompt;
                public FrameLayout eventLayout;
                public ImageView more;
                public RelativeLayout layoutCH;
                public TextView txtCH;
                public ImageView imgCH;
            }

            private void removeItemFromList(int position) {

                DatabaseManager manager = new DatabaseManager(DeviceListActivity.this);
                SQLiteDatabase db = manager.getReadableDatabase();
                Cursor cursor = db.query(DatabaseManager.TABLE_SNAPSHOT, new String[]{"_id", "dev_uid", "file_path", "time"}, "dev_uid = '"
                        + MultiViewActivity.DeviceList.get(position).UID + "'", null, null, null, "_id LIMIT " + MultiViewActivity.CAMERA_MAX_LIMITS);
                while (cursor.moveToNext()) {
                    String file_path = cursor.getString(2);
                    File file = new File(file_path);
                    if (file.exists())
                        file.delete();
                }
                cursor.close();
                db.close();
                manager.removeSnapshotByUID(MultiViewActivity.DeviceList.get(position).UID);
                manager.removeDeviceByUID(MultiViewActivity.DeviceList.get(position).UID);
                MyCamera myCamera = MultiViewActivity.CameraList.get(position);
                DeviceInfo deviceInfo = MultiViewActivity.DeviceList.get(position);

                myCamera.stop(Camera.DEFAULT_AV_CHANNEL);
                myCamera.disconnect();
                myCamera.unregisterIOTCListener(mActivity);

                MultiViewActivity.DeviceList.remove(position);
                MultiViewActivity.CameraList.remove(position);
                this.notifyDataSetChanged();
                MultiViewActivity.removeFromMultiView(deviceInfo.UID, deviceInfo.UUID);
                verifyCameraLimit();
                mCameraList.post(new Runnable() {

                    @Override
                    public void run() {
                        if (MultiViewActivity.DeviceList.size() == 0) {
                            mNullDeviceTxt.setVisibility(View.VISIBLE);

                        }
                    }
                });

            }
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch (v.getId()) {
                //		case R.id.btnWebAdd:
                //			if (MultiViewActivity.CameraList.size() < MultiViewActivity.CAMERA_MAX_LIMITS) {
                //				if (MultiViewActivity.SupportDeviceOnCloud) {
                //
                //					DatabaseManager DatabaseManager = new DatabaseManager(this);
                //					if (DatabaseManager.getLoginPassword().equals("")) {
                //
                //						DialogInterface.OnClickListener okClick = new DialogInterface.OnClickListener() {
                //
                //							@Override
                //							public void onClick(DialogInterface dialog, int which) {
                //								// TODO Auto-generated method stub
                //								DatabaseManager DatabaseManager = new DatabaseManager(DeviceListActivity.this);
                //								DatabaseManager.Logout();
                //								Intent intent = new Intent();
                //								intent.setClass(DeviceListActivity.this, LoginActivity.class);
                //								startActivityForResult(intent, MultiViewActivity.REQUEST_CODE_LOGIN);
                //                                      overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                //								dialog.dismiss();
                //							}
                //						};
                //
                //						DialogInterface.OnClickListener cancelClick = new DialogInterface.OnClickListener() {
                //
                //							@Override
                //							public void onClick(DialogInterface dialog, int which) {
                //								// TODO Auto-generated method stub
                //								dialog.dismiss();
                //							}
                //						};
                //
                //						MultiViewActivity.showSelectDialog(this, getText(R.string.tips_warning),
                //								getText(R.string.txt_please_login_your_cloud_account), getText(R.string.txt_login), getText(R.string.cancel),
                //								okClick, cancelClick);
                //
                //					} else {
                //						Intent intent = new Intent();
                //						intent.setClass(DeviceListActivity.this, CloudDeviceListActivity.class);
                //						startActivityForResult(intent, REQUEST_CODE_CLOUDCAMERA_ADD);
                //                     overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                //					}
                //
                //				} else {
                //				}
                //			}
                //			break;
                //		case R.id.btnLocalAdd:
                //			if (MultiViewActivity.CameraList.size() < MultiViewActivity.CAMERA_MAX_LIMITS) {
                //				Intent intent = new Intent();
                ////				if (MultiViewActivity.SupportEasyWiFiSetting) {  // v1
                ////					intent.setClass(DeviceListActivity.this, SelectCableActivity.class);
                ////				} else {
                ////					intent.setClass(DeviceListActivity.this, AddDeviceActivity.class);
                ////				}
                //				intent.setClass(DeviceListActivity.this, qr_codeActivity.class);
                //				startActivityForResult(intent, REQUEST_CODE_CAMERA_ADD);
                //      overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                //			}
                //			break;
            }
        }

        @Override
        public void receiveFrameDataForMediaCodec(Camera camera, int avChannel, byte[] buf, int length, int pFrmNo, byte[] pFrmInfoBuf, boolean isIframe, int codecId) {
            // TODO Auto-generated method stub

        }

        @Override
        public void btn_onDropbox_click(PopupWindow PopupWindow) {
            // TODO Auto-generated method stub

        }

        @Override
        public void btn_infomation_click(PopupWindow PopupWindow) {
            // TODO Auto-generated method stub

        }

        @Override
        public void btn_log_in_out_click(PopupWindow PopupWindow) {
            // TODO Auto-generated method stub

        }

        @Override
        public void btn_change_ch(int channel) {
            // TODO Auto-generated method stub

        }

        @Override
        public void btn_change_quality(int level) {
            // TODO Auto-generated method stub

        }

        @Override
        public void btn_change_env(int mode) {
            // TODO Auto-generated method stub

        }

        @Override
        public void btn_add_monitor(HashMap<Integer, Boolean> AddMap, int DeviceNo) {
            Bundle extras = new Bundle();
            extras.putString("dev_uid", MultiViewActivity.DeviceList.get(DeviceNo).UID);
            extras.putString("dev_uuid", MultiViewActivity.DeviceList.get(DeviceNo).UUID);
            extras.putString("dev_nickname", MultiViewActivity.DeviceList.get(DeviceNo).NickName);
            extras.putString("conn_status", MultiViewActivity.DeviceList.get(DeviceNo).Status);
            extras.putString("view_acc", MultiViewActivity.DeviceList.get(DeviceNo).View_Account);
            extras.putString("view_pwd", MultiViewActivity.DeviceList.get(DeviceNo).View_Password);
            extras.putString("OriginallyUID", OriginallyUID);
            extras.putInt("OriginallyChannelIndex", OriginallyChannelIndex);
            extras.putSerializable("AddMap", AddMap);
            // add device always channel0
            extras.putInt("camera_channel", MyCamera.DEFAULT_AV_CHANNEL);
            extras.putInt("MonitorIndex", mMonitorIndex);
            Intent Intent = new Intent();
            Intent.putExtras(extras);
            DeviceListActivity.this.setResult(RESULT_OK, Intent);
            DeviceListActivity.this.finish();
            overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        }

        @Override
        public void btn_photo(int value) {

        }

    }
}
