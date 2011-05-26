package edu.mit.mobile.android.locast.ver2.casts;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4_map.app.LoaderManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageLoaderAdapter;
import edu.mit.mobile.android.imagecache.SimpleThumbnailCursorAdapter;
import edu.mit.mobile.android.locast.data.Cast;
import edu.mit.mobile.android.locast.data.CastMedia;
import edu.mit.mobile.android.locast.ver2.R;
import edu.mit.mobile.android.locast.ver2.browser.BrowserHome;
import edu.mit.mobile.android.locast.ver2.itineraries.CastsOverlay;
import edu.mit.mobile.android.locast.ver2.itineraries.LocatableItemOverlay;

public class CastDetail extends LocatableDetail implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener, OnClickListener {
	private LoaderManager mLoaderManager;
	private CastsOverlay mCastsOverlay;
	private MapController mMapController;
	private SimpleCursorAdapter mCastMedia;

	private static final int
		LOADER_CAST = 0,
		LOADER_CAST_MEDIA = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.cast_detail);

		initOverlays();

		mMapController = ((MapView)findViewById(R.id.map)).getController();
		mLoaderManager = getSupportLoaderManager();
		mLoaderManager.initLoader(LOADER_CAST, null, this);
		mLoaderManager.initLoader(LOADER_CAST_MEDIA, null, this);
		findViewById(R.id.home).setOnClickListener(this);
		findViewById(R.id.refresh).setOnClickListener(this);

		final GridView castMediaView = (GridView) findViewById(R.id.cast_media);

		mCastMedia = new SimpleThumbnailCursorAdapter(this,
				R.layout.cast_media_item,
				null,
				new String[]{CastMedia._TITLE, CastMedia._THUMBNAIL, CastMedia._THUMB_LOCAL},
				new int[]{R.id.title, R.id.media_thumbnail, R.id.media_thumbnail},
				new int[]{R.id.media_thumbnail},
				0);

		castMediaView.setAdapter(new ImageLoaderAdapter(this, mCastMedia, ImageCache.getInstance(this), new int[]{R.id.media_thumbnail}, 133, 100, ImageLoaderAdapter.UNIT_DIP));

		castMediaView.setOnItemClickListener(this);

		castMediaView.setEnabled(true);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.home:
			startActivity(new Intent(this, BrowserHome.class));
			break;

		case R.id.refresh:
			startService(new Intent(Intent.ACTION_SYNC, getIntent().getData()));
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {

		final Cursor c = (Cursor) adapter.getItemAtPosition(position);
		final String mediaString = c.getString(c.getColumnIndex(CastMedia._MEDIA_URL));
		final String locMediaString = c.getString(c.getColumnIndex(CastMedia._LOCAL_URI));
		final String mimeType = c.getString(c.getColumnIndex(CastMedia._MIME_TYPE));
		Uri media;

		if (locMediaString != null){
			media = Uri.parse(locMediaString);

		}else if (mediaString != null){
			media = Uri.parse(mediaString);

		}else{
			return;
		}
		final Intent i = new Intent(Intent.ACTION_VIEW, media);
		// setting the MIME type for URLs doesn't work.
		startActivity(i);

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final Uri data = getIntent().getData();
		switch (id){
		case LOADER_CAST:
			return new CursorLoader(this, data, Cast.PROJECTION, null, null, null);
		case LOADER_CAST_MEDIA:
			return new CursorLoader(this, Cast.getCastMediaUri(data), CastMedia.PROJECTION, null, null, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		switch (loader.getId()){
		case LOADER_CAST:
			mCastsOverlay.swapCursor(c);
			if (c.moveToFirst()){
				//MediaProvider.dumpCursorToLog(c, Cast.PROJECTION);
				((TextView)findViewById(R.id.title)).setText(c.getString(c.getColumnIndex(Cast._TITLE)));
				((TextView)findViewById(R.id.author)).setText(c.getString(c.getColumnIndex(Cast._AUTHOR)));
				((TextView)findViewById(R.id.description)).setText(c.getString(c.getColumnIndex(Cast._DESCRIPTION)));
				setPointerFromCursor(c, mMapController);
			}

		break;

		case LOADER_CAST_MEDIA:
			mCastMedia.swapCursor(c);
			/*for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()){
				MediaProvider.dumpCursorToLog(c, CastMedia.PROJECTION);
			}*/
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch(loader.getId()){
		case LOADER_CAST:
			mCastsOverlay.swapCursor(null);
			break;

		case LOADER_CAST_MEDIA:
			mCastMedia.swapCursor(null);
			break;
		}
	}

	@Override
	protected LocatableItemOverlay createItemOverlay() {
		mCastsOverlay = new CastsOverlay(this);
		return mCastsOverlay;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, R.id.favorites, 0, "favorite");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
		case R.id.favorites:
			// XXX do something
			return true;

			default:
				return super.onOptionsItemSelected(item);
		}

	}
}
