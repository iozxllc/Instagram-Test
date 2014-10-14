package com.example.instagramtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;


public class MainActivity extends ActionBarActivity {
	private MainActivity thisMainActivity = this;
	private IGAdapter adapter;
	private boolean retrievingURLS = true;
    
	//using static classes to aid in readability
    public static class IGAdapter extends ArrayAdapter<String> {
        private Activity context;
        public List<String> urls;
        private int screenWidth = 0;
        
        static class ViewHolder {
        	ImageView imageView;
        	String url;
        }
        
		public IGAdapter(Activity context, int resource, List<String> urls) {
			super(context, resource, urls);
			this.context = context;
			this.urls = urls;
			
			Display display = context.getWindowManager().getDefaultDisplay(); 
			screenWidth = display.getWidth();  // deprecated
		}    	
		
		@Override
		public View getView(int position, View rowView, ViewGroup parent) {
			int width = screenWidth;
			int height = width;
			if (position % 3 != 0) {
				height = width/2;
			}
			
			if (rowView == null || !((ViewHolder) rowView.getTag()).url.equals(urls.get(position))) {
				LayoutInflater inflater = context.getLayoutInflater();
				rowView = inflater.inflate(R.layout.ig_view_layout, parent, false);
				Picasso.with(context)
				  .load(urls.get(position))
				  .resize(width, height)
				  .centerCrop()
				  .into((ImageView) rowView.findViewById(R.id.list_image));
				
				((ImageView) rowView.findViewById(R.id.list_image)).getLayoutParams().height = height;
				((ImageView) rowView.findViewById(R.id.list_image)).getLayoutParams().width = width;
				
				ViewHolder holder = new ViewHolder();
				holder.imageView = (ImageView) rowView.findViewById(R.id.list_image);
				holder.url = urls.get(position);
				rowView.setTag(holder);
			}

			return rowView;
		}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final ArrayList<String> urlsList = getLatestURLs();
					
					runOnUiThread(new Runnable() {
					     @Override
					     public void run() {
					    	 final ListView list = (ListView) findViewById(R.id.list_view);
					    	 adapter = new IGAdapter(thisMainActivity, R.layout.ig_view_layout, urlsList);
					    	 list.setAdapter(adapter); 
					    	 
					    	 /*
					    	  * Touch to enlarge implementation
					    	  */
					    	 list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
								@Override
								public void onItemClick(AdapterView<?> parent,
										View view, int position, long id) {
									ImageView iv = (ImageView) view.findViewById(R.id.list_image);
									
									Display display = getWindowManager().getDefaultDisplay(); 
									int screenHeight = display.getHeight();  // deprecated
									iv.getLayoutParams().height = screenHeight;
									iv.getLayoutParams().width = view.getWidth();
									iv.requestLayout();
									Picasso.with(getApplicationContext())
									  .load(adapter.urls.get(position))
									  .resize(view.getWidth(), screenHeight)
									  .centerCrop()
									  .into(iv);

									int[] coords = new int[2];
									view.getLocationInWindow(coords);
									list.scrollTo(0, coords[1]);
								}
					    	 });
					    	 
					    	 /*
					    	  * drag to reorder implementation
					    	  */
					    	 ((TouchInterceptor)list).setDropListener(new TouchInterceptor.DropListener() {
					    		 public void drop(int from, int to) {
					    			 //Assuming that item is moved up the list			
					    			 int direction = -1;			
					    			 int loop_start = from;			
					    			 int loop_end = to;
			
					    			 //For instance where the item is dragged down the list			
					    			 if(from < to) {
					    				 direction = 1;
					    			 }

					    			 String target = adapter.urls.get(from);

					    			 for(int i=loop_start;i!=loop_end;i=i+direction) {
					    				 adapter.urls.set(i, adapter.urls.get(i+direction));
					    			 }

					    			 adapter.urls.set(to, target);

							    	 adapter.notifyDataSetChanged();
					    		 }
					    	 });

					    	 registerForContextMenu(list);
					    	 
					    	 //TODO: endless scroll implementation
					    	 /*list.setOnScrollListener(new OnScrollListener(){
								@Override
								public void onScrollStateChanged(
										AbsListView view, int scrollState) {
									
								}

								@Override
								public void onScroll(AbsListView view,
										int firstVisibleItem,
										int visibleItemCount, int totalItemCount) {
									
									int lastInScreen = firstVisibleItem + visibleItemCount;

									if (lastInScreen == totalItemCount) {
										if (!retrievingURLS) {
											new Thread(new Runnable() {
												@Override
												public void run() {
													adapter.urls.addAll(getLatestURLs());
													runOnUiThread(new Runnable() {
													     @Override
													     public void run() {
													    	 adapter.notifyDataSetChanged();
													     }
													});
												}
											}).start();
										}
									}
								}
							});*/
					     }
					});
			    } catch (Exception e) {
			    	e.printStackTrace();
			    }
			}
		}).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private ArrayList<String> getLatestURLs() {
    	retrievingURLS = true;

		JSONObject json = getJSON();
		
		JSONArray dataArray;
		final ArrayList<String> urlsList = new ArrayList<String>();
		try {
			dataArray = json.getJSONArray("data");
			for (int i = 0; i < dataArray.length(); i++) {
				final String lowResURL = dataArray.getJSONObject(i).getJSONObject("images").getJSONObject("low_resolution").getString("url");
				System.out.println(lowResURL);
				if (adapter == null 
						|| adapter.urls == null 
						|| !adapter.urls.contains(lowResURL)) {
					urlsList.add(lowResURL);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	retrievingURLS = false;
		return urlsList;
    }
    
    private JSONObject getJSON() {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("https://api.instagram.com/v1/tags/selfie/media/recent?access_token=1471048716.1fb234f.c8f52c6133f54abea57fbf07a2fd3a68");
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e(MainActivity.class.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			JSONObject json = new JSONObject(builder.toString());
			return json;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }
}
