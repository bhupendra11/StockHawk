package com.bhupendra.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bhupendra.android.stockhawk.R;
import com.bhupendra.android.stockhawk.data.QuoteColumns;
import com.bhupendra.android.stockhawk.data.QuoteProvider;
import com.bhupendra.android.stockhawk.rest.CustomRecyclerView;
import com.bhupendra.android.stockhawk.rest.QuoteCursorAdapter;
import com.bhupendra.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.bhupendra.android.stockhawk.rest.Utils;
import com.bhupendra.android.stockhawk.service.StockIntentService;
import com.bhupendra.android.stockhawk.service.StockTaskService;
import com.bhupendra.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{



  private CharSequence mTitle;
  private Intent mServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private Context mContext;
  private Cursor mCursor;
  boolean isConnected;

  public static final String ACTION_BAD_STOCK = "com.sam_chordas.android.stockhawk.ACTION_BAD_STOCK";
  private BroadcastReceiver myBroadcastReceiver;

  public static final String BAD_INPUT_EVENT = "bad_input_event";


  private FrameLayout frameLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;


    // handler for received Intents for the "my-event" event
    myBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        // Extract data included in the Intent
        String message = intent.getStringExtra("message");
   if (MyStocksActivity.ACTION_BAD_STOCK.equals(message)) {
          badStockToast();

        }
      }
    };


    frameLayout = (FrameLayout) findViewById(android.R.id.content);

    ConnectivityManager cm =
        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    isConnected = activeNetwork != null &&
        activeNetwork.isConnectedOrConnecting();
    setContentView(R.layout.activity_my_stocks);
    // The intent service is for executing immediate pulls from the Yahoo API
    // GCMTaskService can only schedule tasks, they cannot execute immediately
    mServiceIntent = new Intent(this, StockIntentService.class);
    if (savedInstanceState == null){
      // Run the initialize task service so that some stocks appear upon an empty database
      mServiceIntent.putExtra("tag", "init");
      if (isConnected){
        startService(mServiceIntent);
      } else{
        networkToast();
      }
    }

    //Display no network message when network is not available and old stocks  are displayed
    TextView noNetworkView = (TextView) findViewById(R.id.no_network_view_stock_list);
    if(!isConnected){
      //noNetworkView.setVisibility(View.VISIBLE);

      Snackbar snackbar = Snackbar
              .make(frameLayout, getString(R.string.no_nework_snakbar_string), Snackbar.LENGTH_INDEFINITE)
              .setAction("RETRY", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                  recreate();
                }
              });

      // Changing message text color
      snackbar.setActionTextColor(Color.RED);

      // Changing action button text color
      View sbView = snackbar.getView();
      TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
      textView.setTextColor(Color.YELLOW);

      snackbar.show();

    }


    //Used a simple customRecyclerView which supports emptyView
    CustomRecyclerView recyclerView = (CustomRecyclerView) findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    mCursorAdapter = new QuoteCursorAdapter(this, null);
    recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override public void onItemClick(View v, int position) {

                Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
                mCursor.moveToPosition(position);

                String stockSymbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
                intent.putExtra("symbol", stockSymbol);
                startActivity(intent);

              }
            }));
    TextView mEmptyView = (TextView) findViewById(R.id.recycler_view_stocks_emptyView);
    recyclerView.setEmptyView(mEmptyView);

    recyclerView.setAdapter(mCursorAdapter);



    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(recyclerView);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {

        if (isConnected){
          new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
              .content(R.string.content_test)
              .inputType(InputType.TYPE_CLASS_TEXT)
              .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                  // On FAB click, receive user input. Make sure the stock doesn't already exist
                  // in the DB and proceed accordingly

                  // Here converted the input uppercase before searching for the stock

                    Cursor c = getContentResolver().query(
                            QuoteProvider.Quotes.CONTENT_URI,
                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                            new String[]{input.toString().toUpperCase()}, null);
                    if (c.getCount() != 0) {
                      Toast toast =
                              Toast.makeText(MyStocksActivity.this, getString(R.string.stock_already_saved),
                                      Toast.LENGTH_LONG);
                      toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                      toast.show();
                      return;
                    } else {
                      // Add the stock to DB
                      mServiceIntent.putExtra("tag", "add");
                      mServiceIntent.putExtra("symbol", input.toString().toUpperCase());
                      startService(mServiceIntent);
                    }
                  c.close();


                }
              })
              .show();


        } else {
          networkToast();
        }

      }
    });





    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(recyclerView);

    mTitle = getTitle();
    if (isConnected){
      long period = 3600L;
      long flex = 10L;
      String periodicTag = "periodic";

      // create a periodic task to pull stocks once every hour after the app has been opened. This
      // is so Widget data stays up to date.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
          .setService(StockTaskService.class)
          .setPeriod(period)
          .setFlex(flex)
          .setTag(periodicTag)
          .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
          .setRequiresCharging(false)
          .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }


  }



  @Override
  public void onResume() {
    super.onResume();
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

    // Register mMessageReceiver to receive messages.
    LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver,
            new IntentFilter(BAD_INPUT_EVENT));
  }

  @Override
  protected void onPause() {
    // Unregister since the activity is not visible
    LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadcastReceiver);
    super.onPause();
  }

  public void badStockToast(){
    Toast.makeText(mContext, getString(R.string.bad_stock_input),Toast.LENGTH_SHORT).show();
  }


  public void networkToast(){
    Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }


  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
      restoreActionBar();
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args){
    // This narrows the return to only the stocks that are most current.
    return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
        QuoteColumns.ISCURRENT + " = ?",
        new String[]{"1"},
        null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mCursorAdapter.swapCursor(data);
    mCursor = data;
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }


  @Override
  public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    super.onSaveInstanceState(outState, outPersistentState);
  }
}

