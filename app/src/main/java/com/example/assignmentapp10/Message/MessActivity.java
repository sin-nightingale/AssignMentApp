package com.example.assignmentapp10.Message;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmentapp10.Bluetooth.BlueMActivity;
import com.example.assignmentapp10.MainActivity;
import com.example.assignmentapp10.R;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jxl.Sheet;
import jxl.Workbook;

public class MessActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    public static String TAG = "SMSMANAGER";
    private Button btn_send, btn_improt, btn_send_more;
    private EditText phoneEt, contextEt;
    private RecyclerView rv_list;
    private ProgressDialog progressDialog;
    private List<Person> personList;
    private PersonAdapter mAdapter;
    private TextView tips;
    private RadioGroup rg_all;
    private int IDs = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess);
        init();
    }

    private void init() {
        personList = new ArrayList<>();
        mAdapter = new PersonAdapter();
        btn_send = (Button) this.findViewById(R.id.btn_send);
        tips = (TextView) this.findViewById(R.id.tips);
        btn_send_more = (Button) this.findViewById(R.id.btn_send_more);
        btn_improt = (Button) this.findViewById(R.id.btn_improt);
        phoneEt = (EditText) this.findViewById(R.id.phoneNumberEt);
        contextEt = (EditText) this.findViewById(R.id.contextEt);
        rg_all = (RadioGroup) this.findViewById(R.id.rg_all);
        rv_list = (RecyclerView) this.findViewById(R.id.rv_list);
        progressDialog = new ProgressDialog(this);
        btn_send.setOnClickListener(this);
        btn_improt.setOnClickListener(this);
        btn_send_more.setOnClickListener(this);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        rv_list.setLayoutManager(mLayoutManager);
        rv_list.setItemAnimator(new DefaultItemAnimator());
        rv_list.setAdapter(mAdapter);
        rg_all.setOnCheckedChangeListener(this);
    }
    public void onClick_Return(View view){
        // 在此处编写按钮点击事件的处理逻辑
        Intent[] intents = new Intent[1];
        intents[0] = new Intent(MessActivity.this, MainActivity.class);
        startActivities(intents);
        finish();
    }
    /**
     * 获取 excel 表格中的数据,不能在主线程中调用
     */
    private ArrayList<Person> getXlsData(String filePath, int index) {
        ArrayList<Person> persons = new ArrayList<>();
        try {
            InputStream is = new FileInputStream(filePath);
            Workbook workbook = Workbook.getWorkbook(is);
            Sheet sheet = workbook.getSheet(String.valueOf(index));
            int sheetRows = sheet.getRows();
            for (int i = 0; i < sheetRows; i++) {
                Person person = new Person();
                person.setUserName(sheet.getCell(0, i).getContents());
                person.setPhoneNumber(sheet.getCell(1, i).getContents());
                person.setMsgContent(sheet.getCell(2, i).getContents());
                persons.add(person);
            }
            workbook.close();
        } catch (Exception e) {
            Log.d(TAG, "数据读取错误=" + e);
        }
        return persons;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 导入Excel表格
            case R.id.btn_improt:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
                break;
            // 发送
            case R.id.btn_send:
                String phone = phoneEt.getText().toString().trim();
                String context = contextEt.getText().toString().trim();

                if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(context)) {
                    Toast.makeText(MessActivity.this, "号码或内容不能为空！", Toast.LENGTH_SHORT);
                    return;
                }
                sendSms(IDs, phone, context);

                phoneEt.setText("");
                contextEt.setText("");
                Toast.makeText(getApplicationContext(), "发送完毕", Toast.LENGTH_SHORT).show();
                break;
            // 批量发送
            case R.id.btn_send_more:
                for (Person item : personList) {
                    sendSms(IDs, item.getPhoneNumber(), item.getMsgContent());
                    // 停顿1s
                }
                Toast.makeText(getApplicationContext(), "正在发送,请稍后...", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setupData(List<Person> persons) {
        personList.clear();
        personList.addAll(persons);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_one:
                IDs = 0;
                break;
            case R.id.rb_two:
                IDs = 1;
                break;
        }
    }

    // 异步获取Excel数据信息
    private class ExcelDataLoader extends AsyncTask<String, Void, ArrayList<Person>> {
        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Excel数据导入中,请稍后......");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected ArrayList<Person> doInBackground(String... params) {
            return getXlsData(params[0], 0);
        }

        @Override
        protected void onPostExecute(ArrayList<Person> persons) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (persons != null && persons.size() > 0) {
                // 列表显示数据
                setupData(persons);
            } else {
                // 加载失败
                Toast.makeText(MessActivity.this, "数据加载失败！", Toast.LENGTH_SHORT);
            }
        }
    }

    private class PersonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        class TextViewHolder extends RecyclerView.ViewHolder {
            TextView tv_name, tv_number, tv_content;
            LinearLayout ll_index;

            public TextViewHolder(View itemView) {
                super(itemView);
                tv_name = (TextView) itemView.findViewById(R.id.tv_name);
                tv_number = (TextView) itemView.findViewById(R.id.tv_number);
                tv_content = (TextView) itemView.findViewById(R.id.tv_content);
                ll_index = (LinearLayout) itemView.findViewById(R.id.ll_index);
            }
        }

        @Override
        public TextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TextViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_person, parent, false));
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
            if (holder instanceof TextViewHolder) {
                ((TextViewHolder) holder).tv_name.setText(personList.get(position).getUserName());
                ((TextViewHolder) holder).tv_number.setText(personList.get(position).getPhoneNumber());
                ((TextViewHolder) holder).tv_content.setText(personList.get(position).getMsgContent());
                ((TextViewHolder) holder).ll_index.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        phoneEt.setText(personList.get(position).getPhoneNumber());
                        contextEt.setText(personList.get(position).getMsgContent());
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return personList.size();
        }
    }

    // 获取本地Excel信息
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                Uri uri = data.getData();
                String path = uri.getPath().toString();
                tips.setText(path);

                // 执行Excel数据导入
                new ExcelDataLoader().execute(path.trim());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void sendSms(final int which, String phone, String context) {
        SubscriptionInfo sInfo = null;

        final SubscriptionManager sManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        List<SubscriptionInfo> list = sManager.getActiveSubscriptionInfoList();

        if (list.size() == 2) {
            // 双卡
            sInfo = list.get(which);
        } else {
            // 单卡
            sInfo = list.get(0);
        }

        if (sInfo != null) {
            int subId = sInfo.getSubscriptionId();
            SmsManager manager = SmsManager.getSmsManagerForSubscriptionId(subId);

            if (!TextUtils.isEmpty(phone)) {
                ArrayList<String> messageList =manager.divideMessage(context);
                for(String text:messageList){
                    manager.sendTextMessage(phone, null, text, null, null);
                }
                Toast.makeText(this, "信息正在发送，请稍候", Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(this, "无法正确的获取SIM卡信息，请稍候重试",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}