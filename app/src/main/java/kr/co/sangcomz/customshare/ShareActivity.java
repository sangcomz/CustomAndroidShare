package kr.co.sangcomz.customshare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
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

import java.util.List;

public class ShareActivity extends Activity {

    RecyclerView recyclerView;
    GridLayoutManager gridLayoutManager;
    DoShareAdapter doShareAdapter;
    RelativeLayout shareArea;
    RelativeLayout finishArea;

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

        finishArea = (RelativeLayout) findViewById(R.id.finish_are);
        finishArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void animRelative(final int isUp) {
        ViewCompat.animate(shareArea)
                .setInterpolator(new FastOutLinearInInterpolator())
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
                        sendIntent(position);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return resInfo.size();
        }

        public void sendIntent(int position) {
            Intent targetedShareIntent = (Intent) shareIntent.clone();
            targetedShareIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
            targetedShareIntent.putExtra(Intent.EXTRA_TEXT, "Text");
            targetedShareIntent.setPackage(resInfo.get(position).activityInfo.packageName);
            targetedShareIntent.setClassName(resInfo.get(position).activityInfo.packageName,
                    resInfo.get(position).activityInfo.name);
            context.startActivity(targetedShareIntent);
        }

    }
}
