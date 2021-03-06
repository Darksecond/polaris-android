package agersant.polaris.features.browse;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;

import agersant.polaris.CollectionItem;
import agersant.polaris.PlaybackQueue;
import agersant.polaris.PolarisApplication;
import agersant.polaris.PolarisState;
import agersant.polaris.R;
import agersant.polaris.api.API;
import agersant.polaris.api.ItemsCallback;
import agersant.polaris.api.remote.ServerAPI;
import agersant.polaris.features.PolarisActivity;

public class BrowseActivity extends PolarisActivity {

	public static final String PATH = "PATH";
	public static final String NAVIGATION_MODE = "NAVIGATION_MODE";
	private ProgressBar progressBar;
	private View errorMessage;
	private ViewGroup contentHolder;
	private ItemsCallback fetchCallback;
	private NavigationMode navigationMode;
	private SwipyRefreshLayout.OnRefreshListener onRefresh;
	private ArrayList<? extends CollectionItem> items;
	private API api;
	private ServerAPI serverAPI;
	private PlaybackQueue playbackQueue;

	public BrowseActivity() {
		super(R.string.collection, R.id.nav_collection);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_browse);
		super.onCreate(savedInstanceState);

		PolarisState state = PolarisApplication.getState();
		api = state.api;
		serverAPI = state.serverAPI;
		playbackQueue = state.playbackQueue;

		errorMessage = findViewById(R.id.browse_error_message);
		progressBar = findViewById(R.id.progress_bar);
		contentHolder = findViewById(R.id.browse_content_holder);

		final BrowseActivity that = this;
		fetchCallback = new ItemsCallback() {
			@Override
			public void onSuccess(final ArrayList<? extends CollectionItem> items) {
				that.runOnUiThread(() -> {
					that.progressBar.setVisibility(View.GONE);
					that.items = items;
					that.displayContent();
				});
			}

			@Override
			public void onError() {
				that.runOnUiThread(() -> {
					progressBar.setVisibility(View.GONE);
					errorMessage.setVisibility(View.VISIBLE);
				});
			}
		};

		Intent intent = getIntent();
		navigationMode = (NavigationMode) intent.getSerializableExtra(BrowseActivity.NAVIGATION_MODE);

		if (navigationMode == NavigationMode.RANDOM) {
			onRefresh = (SwipyRefreshLayoutDirection direction) -> loadContent();
		}

		loadContent();
	}

	private void loadContent() {
		progressBar.setVisibility(View.VISIBLE);
		errorMessage.setVisibility(View.GONE);
		Intent intent = getIntent();
		switch (navigationMode) {
			case PATH: {
				String path = intent.getStringExtra(BrowseActivity.PATH);
				if (path == null) {
					path = "";
				}
				loadPath(path);
				break;
			}
			case RANDOM: {
				loadRandom();
				break;
			}
			case RECENT: {
				loadRecent();
				break;
			}
		}
	}

	@SuppressWarnings("UnusedParameters")
	public void retry(View view) {
		loadContent();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	private void loadPath(String path) {
		api.browse(path, fetchCallback);
	}

	private void loadRandom() {
		serverAPI.getRandomAlbums(fetchCallback);
	}

	private void loadRecent() {
		serverAPI.getRecentAlbums(fetchCallback);
	}

	private DisplayMode getDisplayModeForItems(ArrayList<? extends CollectionItem> items) {
		if (items.isEmpty()) {
			return DisplayMode.EXPLORER;
		}

		String album = items.get(0).getAlbum();
		boolean allSongs = true;
		boolean allDirectories = true;
		boolean allHaveArtwork = true;
		boolean allHaveAlbum = album != null;
		boolean allSameAlbum = true;
		for (CollectionItem item : items) {
			allSongs &= !item.isDirectory();
			allDirectories &= item.isDirectory();
			allHaveArtwork &= item.getArtwork() != null;
			allHaveAlbum &= item.getAlbum() != null;
			allSameAlbum &= album != null && album.equals(item.getAlbum());
		}

		if (allDirectories && allHaveArtwork && allHaveAlbum) {
			return DisplayMode.DISCOGRAPHY;
		}

		if (album != null && allSongs && allSameAlbum) {
			return DisplayMode.ALBUM;
		}

		return DisplayMode.EXPLORER;
	}

	private enum DisplayMode {
		EXPLORER,
		DISCOGRAPHY,
		ALBUM,
	}

	enum NavigationMode {
		PATH,
		RANDOM,
		RECENT,
	}

	private void displayContent() {
		if (items == null) {
			return;
		}

		BrowseViewContent contentView = null;
		switch (getDisplayModeForItems(items)) {
			case EXPLORER:
				contentView = new BrowseViewExplorer(this, api, playbackQueue);
				break;
			case ALBUM:
				contentView = new BrowseViewAlbum(this, api, playbackQueue);
				break;
			case DISCOGRAPHY:
				contentView = new BrowseViewDiscography(this, api, playbackQueue);
				break;
		}

		contentView.setItems(items);
		contentView.setOnRefreshListener(onRefresh);

		contentHolder.removeAllViews();
		contentHolder.addView(contentView);
	}
}
