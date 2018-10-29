package agersant.polaris.features.browse;

import android.view.View;
import android.widget.TextView;

import java.util.Locale;

import agersant.polaris.CollectionItem;
import agersant.polaris.PolarisService;
import agersant.polaris.R;


class BrowseItemHolderAlbumTrack extends BrowseItemHolder {

	private final TextView trackNumberText;
	private final TextView titleText;

	BrowseItemHolderAlbumTrack(PolarisService service, BrowseAdapter adapter, View itemView, View itemQueueStatusView) {
		super(service, adapter, itemView, itemQueueStatusView);
		trackNumberText = (TextView) itemView.findViewById(R.id.track_number);
		titleText = (TextView) itemView.findViewById(R.id.title);
	}

	@Override
	void bindItem(CollectionItem item) {
		super.bindItem(item);

		String title = item.getTitle();
		if (title != null) {
			titleText.setText(title);
		} else {
			titleText.setText(item.getName());
		}

		Integer trackNumber = item.getTrackNumber();
		if (trackNumber != null) {
			trackNumberText.setText(String.format((Locale) null, "%1$02d.", trackNumber));
		} else {
			trackNumberText.setText("");
		}
	}

}