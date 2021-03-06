package com.bhupendra.android.stockhawk.rest;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bhupendra.android.stockhawk.R;
import com.bhupendra.android.stockhawk.data.QuoteColumns;
import com.bhupendra.android.stockhawk.data.QuoteProvider;
import com.bhupendra.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.bhupendra.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

/**
 * Created by sam_chordas on 10/6/15.
 *  Credit to skyfishjy gist:
 *    https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
    implements ItemTouchHelperAdapter{

  private static Context mContext;
  private static Typeface robotoLight;
  private boolean isPercent;
  public static final String ACTION_DATA_UPDATED = "com.bhupendra.android.stockhawk.ACTION_DATA_UPDATED";

  public QuoteCursorAdapter(Context context, Cursor cursor){
    super(context, cursor);
    mContext = context;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
    robotoLight = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf");
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_item_quote, parent, false);
    ViewHolder vh = new ViewHolder(itemView);
    return vh;
  }

  @Override
  public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor){

    String symbol = cursor.getString(cursor.getColumnIndex("symbol"));
    String bidPrice = cursor.getString(cursor.getColumnIndex("bid_price"));
    String change ="";

    viewHolder.symbol.setText(symbol);
    viewHolder.symbol.setContentDescription(mContext.getString(R.string.a11y_stock_symbol,symbol));


    viewHolder.bidPrice.setText(cursor.getString(cursor.getColumnIndex("bid_price")));
    viewHolder.bidPrice.setContentDescription(mContext.getString(R.string.a11y_bid_price,bidPrice));

    int sdk = Build.VERSION.SDK_INT;
    if (cursor.getInt(cursor.getColumnIndex("is_up")) == 1){
      if (sdk < Build.VERSION_CODES.JELLY_BEAN){
        viewHolder.change.setBackgroundDrawable(
            mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
      }else {
        viewHolder.change.setBackground(
            mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
      }
    }

    else{
      if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
        viewHolder.change.setBackgroundDrawable(
            mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
      } else{
        viewHolder.change.setBackground(
            mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
      }
    }
    if (Utils.showPercent){
      change = cursor.getString(cursor.getColumnIndex("percent_change"));
      viewHolder.change.setText(change);
     viewHolder.change.setContentDescription(mContext.getString(R.string.a11y_change,change));

    } else{
      change = cursor.getString(cursor.getColumnIndex("change"));
      viewHolder.change.setText(change);
      viewHolder.change.setContentDescription(mContext.getString(R.string.a11y_change,change));
    }
  }

  @Override public void onItemDismiss(int position) {
    Cursor c = getCursor();
    c.moveToPosition(position);
    String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
    mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
    notifyItemRemoved(position);

    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED).setPackage(mContext.getPackageName());
    mContext.sendBroadcast(dataUpdatedIntent);

    if (c.getCount() == 1) {
      Toast.makeText(mContext, R.string.all_stocks_deleted, Toast.LENGTH_SHORT).show();
    }
  }

  @Override public int getItemCount() {
    return super.getItemCount();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder
      implements ItemTouchHelperViewHolder, View.OnClickListener{
    public final TextView symbol;
    public final TextView bidPrice;
    public final TextView change;
    public ViewHolder(View itemView){
      super(itemView);
      symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
      symbol.setTypeface(robotoLight);
      bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
      change = (TextView) itemView.findViewById(R.id.change);
    }

    @Override
    public void onItemSelected(){
      itemView.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onItemClear(){
      itemView.setBackgroundColor(0);
    }

    @Override
    public void onClick(View v) {

    }
  }
}
