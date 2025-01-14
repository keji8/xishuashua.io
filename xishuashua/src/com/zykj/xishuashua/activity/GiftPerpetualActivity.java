package com.zykj.xishuashua.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.zykj.xishuashua.BaseActivity;
import com.zykj.xishuashua.R;
import com.zykj.xishuashua.adapter.CommonAdapter;
import com.zykj.xishuashua.adapter.GiftAdapter;
import com.zykj.xishuashua.adapter.ViewHolder;
import com.zykj.xishuashua.http.EntityHandler;
import com.zykj.xishuashua.http.HttpUtils;
import com.zykj.xishuashua.model.Gift;
import com.zykj.xishuashua.model.Interest;
import com.zykj.xishuashua.utils.CommonUtils;
import com.zykj.xishuashua.utils.StringUtil;
import com.zykj.xishuashua.utils.Tools;
import com.zykj.xishuashua.view.HorizontalListView;
import com.zykj.xishuashua.view.MyRequestDailog;
import com.zykj.xishuashua.view.SegmentThreeView;
import com.zykj.xishuashua.view.SegmentThreeView.onSegmentViewClickListener;
import com.zykj.xishuashua.view.XListView;
import com.zykj.xishuashua.view.XListView.IXListViewListener;

/**
 * @author Administrator
 * 首页永久红包
 */
public class GiftPerpetualActivity extends BaseActivity implements IXListViewListener,OnItemClickListener,onSegmentViewClickListener{

	private HorizontalListView gift_hlistview;
	private XListView mListView;//红包列表
	private TextView gift_allnum;
	private SegmentThreeView order_seg;//App、商家、者个人
	
	private static final String NUM = "10";//每页显示条数
	private int page = 1;//当前第几页
	private String grade_id = "app";//2-个人红包  1-商家红包  app-app红包
	private String interesttag;//兴趣标签
	private List<Gift> gifts = new ArrayList<Gift>();//红包数据
	private GiftAdapter adapter;//设配器
	private CommonAdapter<Interest> iAdapter;
	private Handler mHandler;//异步加载或刷新
	private String[] interestIds;//用户爱好标签

	private List<Interest> interest0 = new ArrayList<Interest>();//全部标签
	private List<Interest> interest1 = new ArrayList<Interest>();//用户爱好标签
	private List<Interest> interest2 = new ArrayList<Interest>();//其他标签
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_gift_perpetual);
		interestIds = getIntent().getStringExtra("interestIds").split(",");
		
		mHandler = new Handler();
		initView();
	}

	/**
	 * 首次加载页面
	 */
	private void initView() {
		findViewById(R.id.aci_back_btn).setOnClickListener(this);;//公司简介
		order_seg = (SegmentThreeView)findViewById(R.id.order_seg);//即时红包分类
		order_seg.setSegmentText("App红包", 0);
		order_seg.setSegmentText("商家红包", 1);
		order_seg.setSegmentText("个人红包", 2);
		order_seg.setOnSegmentViewClickListener(this);
		
		gift_hlistview = (HorizontalListView)findViewById(R.id.gift_hlistview);//标签切换
		gift_allnum = (TextView)findViewById(R.id.gift_allnum);//未抢红包总数
		mListView = (XListView)findViewById(R.id.gift_listview);//选择标签
        adapter = new GiftAdapter(this, R.layout.ui_item_gift, gifts);
        mListView.setAdapter(adapter);
		mListView.setPullLoadEnable(true);
		mListView.setXListViewListener(this);
		mListView.setOnItemClickListener(this);
		requestInterest();
		iAdapter = new CommonAdapter<Interest>(this, R.layout.ui_item_hlabel, interest1) {
			@Override
			public void convert(ViewHolder holder, Interest interest) {
				holder.setText(R.id.interest_num, StringUtil.toString(interest.getCount(), "0"));
				TextView button = holder.getView(R.id.interest_name);
				button.setText(interest.getInterest_name());
				button.setSelected(interest.isChecked());
			}
		};
		gift_hlistview.setAdapter(iAdapter);
		gift_hlistview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View convertView, int position, long checkId) {
				if("more".equals(interest1.get(position).getInterest_id())){
					interest1.remove(interest1.get(position));
					interest1.addAll(interest2);
					iAdapter.notifyDataSetChanged();
				}else{
					for (int i = 0; i < interest1.size(); i++) {
						interest1.get(i).setChecked(false);
					}
					interest1.get(position).setChecked(true);
					iAdapter.notifyDataSetChanged();
					page = 1;
					interesttag = interest1.get(position).getInterest_id();
					requestData();
				}
			}
		});
	}


	/**
	 * 请求网络数据----标签
	 */
	private void requestInterest() {
    	RequestParams params = new RequestParams();
    	params.put("marketprice", "0");//"1"即时红包, "0"永久红包
    	params.put("grade_id", grade_id);//2-个人红包  1-商家红包  app-app红包(默认)
		HttpUtils.getAllInterests(getAllInterests, params);
	}

	/**
	 * 请求网络数据----红包
	 */
	private void requestData(){
		adapter.setGrade_id(grade_id);
    	RequestParams params = new RequestParams();
    	params.put("page", String.valueOf(page));
    	params.put("per_page", NUM);
    	params.put("marketprice", "0");//"1"即时红包, "0"永久红包
    	params.put("interestid", interesttag == null?"1":interesttag);//兴趣标签Id
    	params.put("grade_id", grade_id);//2-个人红包  1-商家红包  app-app红包(默认)
		MyRequestDailog.showDialog(this, "");
		HttpUtils.getsomekindenvelist(rel_getEnveList, params);
	}
	
	/**
	 * 网络请求标签列表
	 * getAllInterests 所有标签
	 */
	private AsyncHttpResponseHandler getAllInterests = new EntityHandler<Interest>(Interest.class) {
		@Override
		public void onReadSuccess(List<Interest> list) {
			requestData();
			screeningChecked(list);
			if(interest1.size()<5){
				/** 加载所有兴趣标签 */
				interest1.addAll(interest0);
				interest1.get(0).setChecked(true);
				iAdapter.notifyDataSetChanged();
			}else{
				/** 加载用户爱好兴趣标签 */
				if(interest1.size() < list.size()){
					Interest interest = new Interest();
					interest.setInterest_id("more");
					interest.setInterest_name("更多");
					interest1.add(interest);
				}
				interest1.get(0).setChecked(true);
				iAdapter.notifyDataSetChanged();
			}
		}
	};
	
	/**
	 * @param list 总标签
	 * 筛选用户兴趣标签
	 */
	private void screeningChecked(List<Interest> list){
		interest0.clear();
		interest1.clear();
		interest2.clear();
		interest0.addAll(list);
		interest2.addAll(list);
		int all_num = 0;
		for (int i = 0; i < list.size(); i++) {
			Interest interest = list.get(i);
			int a =  Integer.valueOf(interest.getCount());
			all_num = all_num + a;
			for (String interest_id : interestIds) {
				if(interest_id.equals(list.get(i).getInterest_id())){
					interest1.add(list.get(i));
					interest2.remove(list.get(i));
				}
			}
		}
		gift_allnum.setText("你有"+all_num+"个红包未领取!");
	}
	
	/**
	 * 获取红包列表
	 */
	private AsyncHttpResponseHandler rel_getEnveList = new EntityHandler<Gift>(Gift.class) {
		@Override
		public void onReadSuccess(List<Gift> list) {
			MyRequestDailog.closeDialog();
			if(page == 1){gifts.clear();}
			gifts.addAll(list);
			adapter.notifyDataSetChanged();
		}
	};

	@Override
	public void onClick(View view) {
		if(view.getId() == R.id.aci_back_btn){ finish(); }
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View convertView, int position, long selectId) {
		if(!CommonUtils.CheckLogin()){ Tools.toast(this, "请先登录"); return; }
		Intent detailIntent = new Intent(this, GiftDetailActivity.class);
		detailIntent.putExtra("goods_id", gifts.get(position-1).getGoods_id()).putExtra("saw", gifts.get(position-1).getSaw());;
		startActivity(detailIntent);
	}

	@Override
	public void onRefresh() {
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				page = 1;
				requestData();
				onLoad();
			}
		}, 1000);
	}

	@Override
	public void onLoadMore() {
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				page += 1;
				requestData();
				onLoad();
			}
		}, 1000);
	}

	private void onLoad() {
		mListView.stopRefresh();
		mListView.stopLoadMore();
		mListView.setRefreshTime("刚刚");
	}

	@Override
	public void onSegmentViewClick(View convertView, final int position) {
		gift_hlistview.setSelection(0);
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(position == 0){
					page = 1;
					gift_hlistview.setVisibility(View.GONE);
					grade_id = "app";
					requestInterest();
					requestData();
					onLoad();
				}else if(position == 1){
					page = 1;
					gift_hlistview.setVisibility(View.VISIBLE);
					grade_id = "1";
					requestInterest();
					requestData();
					onLoad();
				}else{
					page = 1;
					gift_hlistview.setVisibility(View.VISIBLE);
					grade_id = "2";
					requestInterest();
					requestData();
					onLoad();
				}
			}
		}, 100);
	}
}
