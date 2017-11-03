package doext.implement;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoImageLoadHelper;
import core.helper.DoImageLoadHelper.OnPostExecuteListener;
import core.helper.DoJsonHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.helper.cache.DoCacheManager;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.define.do_ImageBrowserView_IMethod;
import doext.define.do_ImageBrowserView_MAbstract;
import doext.imagebrowserview.HackyViewPager;
import doext.imagebrowserview.PhotoView;
import doext.imagebrowserview.PhotoViewAttacher.OnViewTapListener;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,
 * do_ImageBrowserView_IMethod接口； #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_ImageBrowserView_View extends HackyViewPager implements DoIUIModuleView, do_ImageBrowserView_IMethod {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_ImageBrowserView_MAbstract model;
	private int mIndex;
	private SamplePagerAdapter mAdapter;
	private Context mContext;

	public do_ImageBrowserView_View(Context context) {
		super(context);
		this.mContext = context;
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_ImageBrowserView_MAbstract) _doUIModule;
		this.setOnPageChangeListener(new MyPageChangeListener());
		mAdapter = new SamplePagerAdapter();
		this.setAdapter(mAdapter);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("index")) {
			mIndex = DoTextHelper.strToInt(_changedValues.get("index"), 0);
			setIndex();
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("bindItems".equals(_methodName)) {
			bindItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
	}

	private void setIndex() {
		if (mIndex < 0) {
			mIndex = 0;
		}
		if (mAdapter.getCount() > 0) {
			int _maxCount = mAdapter.getCount() - 1;
			if (mIndex > _maxCount) {
				mIndex = _maxCount;
			}
			this.setCurrentItem(mIndex, false);
		}
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 绑定数据；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void bindItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		JSONArray _data = DoJsonHelper.getJSONArray(_dictParas, "data");
		if (_data == null || _data.length() <= 0) {
			throw new Exception("data 参数不能为空！");
		}
		mAdapter.bindData(_data);
		setIndex();
	}

	private class SamplePagerAdapter extends PagerAdapter {

		private JSONArray mData;

		public void bindData(JSONArray _array) {
			this.mData = _array;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if (mData == null) {
				return 0;
			}
			return mData.length();
		}

		@Override
		public View instantiateItem(ViewGroup container, final int position) {
			RelativeLayout rootLayout = new RelativeLayout(mContext);
			final PhotoView iv = new PhotoView(mContext);
			rootLayout.addView(iv, new RelativeLayout.LayoutParams(-1, -1));

			final ProgressBar pb = new ProgressBar(mContext);
			RelativeLayout.LayoutParams pb_lp = new RelativeLayout.LayoutParams(-2, -2);
			pb_lp.addRule(RelativeLayout.CENTER_IN_PARENT);
			pb.setVisibility(View.GONE);
			rootLayout.addView(pb, pb_lp);

			iv.setMaxScale(5.0f);
			iv.setTag(false);
			iv.setOnViewTapListener(new OnViewTapListener() {
				@Override
				public void onViewTap(View view, float x, float y) {
					fireTouch(position);
				}
			});
			iv.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					fireLongTouch(position);
					return false;
				}
			});
			try {
				JSONObject _childData = mData.getJSONObject(position);
				final String _init = DoJsonHelper.getString(_childData, "init", "");
				final String _source = DoJsonHelper.getString(_childData, "source", "");

				//先判断source是否是本地图片，如果是本地图片就不需要加载init 的图片
				if (null != DoIOHelper.getHttpUrlPath(_source)) {
					Bitmap bitmap = DoCacheManager.getInstance().getBitmapFromMemoryCache(_source, true);
					if (bitmap != null) {
						iv.setImageBitmap(bitmap);
					} else { //表示从没加载过，先去取init 的图片
						//TODO 由于当前请求网络图片都是串行的 先取本地的缩略图
						pb.setVisibility(View.VISIBLE);
						if (null != DoIOHelper.getHttpUrlPath(_init)) { //从远程取http 图片
							bitmap = DoCacheManager.getInstance().getBitmapFromMemoryCache(_init, true);
							if (bitmap != null) { //先从本地缓存里面取，如果取到的缩略图不为空
								iv.setImageBitmap(bitmap);
							} else {
								DoImageLoadHelper.getInstance().loadURL(_init, "always", 100, 100, new OnPostExecuteListener() {
									@Override
									public void onResultExecute(Bitmap bitmap, String url) {
										boolean isSucess = (Boolean) iv.getTag();
										if ((bitmap != null && url.equals(_init)) && !isSucess) {
											iv.setImageBitmap(bitmap);
										}
									}
								});
							}
						} else { //从本地取图片
							setLocalImage(_init, iv);
						}
						
						DoImageLoadHelper.getInstance().loadURL(_source, "always", (int) model.getWidth(), (int) model.getHeight(), new OnPostExecuteListener() {
							@Override
							public void onResultExecute(Bitmap bitmap, String url) {
								//url.equals(source)判断source等于最后请求结果URL并显示，忽略掉中间线程结果；
								if ((bitmap != null && url.equals(_source))) {
									iv.setTag(true);
									iv.setImageBitmap(bitmap);
									pb.setVisibility(View.GONE);
								}
							}
						});
					}
				} else {
					setLocalImage(_source, iv);
				}
				container.addView(rootLayout, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
			}
			return rootLayout;
		}

		private void setLocalImage(String local, ImageView iv) throws Exception {
			if (local != null && !"".equals(local)) {
				String path = DoIOHelper.getLocalFileFullPath(model.getCurrentPage().getCurrentApp(), local);
				Bitmap bitmap = DoImageLoadHelper.getInstance().loadLocal(path, (int) model.getWidth(), (int) model.getHeight());
				iv.setImageBitmap(bitmap);
			}
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
	}

	private class MyPageChangeListener implements ViewPager.OnPageChangeListener {

		@Override
		public void onPageScrollStateChanged(int state) {
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			if (positionOffset == 0.0 && positionOffsetPixels == 0) {
				getParent().requestDisallowInterceptTouchEvent(false);
			}
		}

		@Override
		public void onPageSelected(int position) {
			try {
				model.setPropertyValue("index", position + "");
				DoInvokeResult invokeResult = new DoInvokeResult(model.getUniqueKey());
				invokeResult.setResultInteger(position);
				model.getEventCenter().fireEvent("indexChanged", invokeResult);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void fireTouch(int index) {
		DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
		JSONObject _obj = new JSONObject();
		try {
			_obj.put("index", index);
		} catch (Exception e) {
		}
		_invokeResult.setResultNode(_obj);
		model.getEventCenter().fireEvent("touch", _invokeResult);
	}

	private void fireLongTouch(int index) {
		DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
		JSONObject _obj = new JSONObject();
		try {
			_obj.put("index", index);
		} catch (Exception e) {
		}
		_invokeResult.setResultNode(_obj);
		model.getEventCenter().fireEvent("longTouch", _invokeResult);
	}
}