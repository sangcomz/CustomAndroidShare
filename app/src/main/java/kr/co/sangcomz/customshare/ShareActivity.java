package kr.co.sangcomz.customshare;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ShareActivity extends Activity {

    RecyclerView recyclerView;
    GridLayoutManager gridLayoutManager;
    DoShareAdapter doShareAdapter;
    RelativeLayout shareArea;
    RelativeLayout finishArea;

    String filePath;
    String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        shareArea = (RelativeLayout) findViewById(R.id.share_area);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        gridLayoutManager = new GridLayoutManager(this, 3);
        doShareAdapter = new DoShareAdapter(ShareActivity.this);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(doShareAdapter);
        animRelative(-1);

        final long unixTime = System.currentTimeMillis() / 1000L;
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp_custom_share/";
        fileName = unixTime + "temp_customShare.jpg";

        finishArea = (RelativeLayout) findViewById(R.id.finish_are);
        finishArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        startExtMediaScan(this);
        File file = new File(filePath, fileName);
        if (file.exists())
            file.delete();

    }

    public static void startExtMediaScan(Context mContext) {
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    }


    private void animRelative(final int isUp) {
        ViewCompat.animate(shareArea)
//                .setInterpolator(AnimUtils.FAST_OUT_SLOW_IN_INTERPOLATOR) //사라지는 모양
                .setInterpolator(new FastOutLinearInInterpolator())
//                .setInterpolator(AnimUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR)
                .translationYBy(isUp * convertDP(this, -300))

                .setDuration(300)
                .withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        if (isUp == 1)
                            shareArea.setVisibility(View.VISIBLE);
                    }
                })
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (isUp == -1)
                            animRelative(1);
                    }
                })
                .withLayer()
                .start();
    }

    public static int convertDP(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }


    public class DoShareAdapter
            extends RecyclerView.Adapter<DoShareAdapter.ViewHolder> {
        Context context;
        Intent shareIntent = new Intent();
        List<ResolveInfo> resInfo;
        PackageManager pm;

        public class ViewHolder extends RecyclerView.ViewHolder {

            public final RelativeLayout areaShare;
            public final ImageView imgShare;
            public final TextView txtShare;


            public ViewHolder(View view) {
                super(view);
                areaShare = (RelativeLayout) view.findViewById(R.id.share_area);
                imgShare = (ImageView) view.findViewById(R.id.share_ic);
                txtShare = (TextView) view.findViewById(R.id.share_text);
            }
        }

        public DoShareAdapter(Context context) {
            this.context = context;
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            pm = context.getPackageManager();
            resInfo = pm.queryIntentActivities(shareIntent, 0);

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_share_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.txtShare.setText(resInfo.get(position).loadLabel(pm));
            holder.imgShare.setImageDrawable(resInfo.get(position).loadIcon(pm));
            holder.areaShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (resInfo.get(position).activityInfo.packageName.contains("kakao.talk")) {
                        //카카오톡일경우에 할 액션.
                        System.out.println("카카오톡!");
                    } else {
                        File file = new File(filePath, fileName);
                        if (file.exists())
                            sendIntent(file, position);
                        else
                            setPackageFolderProfileImageSave(context, "YOUR_IMAGE_URL", position);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return resInfo.size();
        }

        public void sendIntent(File file, int position) {
            Intent targetedShareIntent = (Intent) shareIntent.clone();
            targetedShareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            targetedShareIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
            targetedShareIntent.putExtra(Intent.EXTRA_TEXT, "Text");
            targetedShareIntent.setPackage(resInfo.get(position).activityInfo.packageName);
            targetedShareIntent.setClassName(resInfo.get(position).activityInfo.packageName,
                    resInfo.get(position).activityInfo.name);
            context.startActivity(targetedShareIntent);
        }

        // 인터넷 파일 저장.
        public void setPackageFolderProfileImageSave(final Context context, String url, final int position) {
            final ProgressDialog progressDialog = new ProgressDialog(context, android.R.style.Theme_Material_Dialog_Alert);
            progressDialog.setMessage("잠시만 기다려주세요.");
            progressDialog.show();
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(url, new FileAsyncHttpResponseHandler(context) {
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {

                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    // Do something with the file `response`
                    Bitmap myBitmap = BitmapFactory.decodeFile(response.getAbsolutePath());
//
                    File file = SaveBitmapToFileCache(myBitmap,
                            filePath, fileName);
                    sendIntent(file, position);
                    progressDialog.dismiss();
                }
            });


        }

        // Bitmap to File
        public File SaveBitmapToFileCache(Bitmap bitmap, String strFilePath,
                                          String filename) {
            File file = new File(strFilePath);


            // If no folders
            if (!file.exists()) {
                file.mkdirs();
                // Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
            }
            File fileCacheItem = new File(strFilePath + filename);
            OutputStream out = null;

            try {
                fileCacheItem.createNewFile();
                out = new FileOutputStream(fileCacheItem);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return fileCacheItem;
        }
    }

}
