package org.benews;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.benews.libbsonj.BsonProxy;

import java.util.HashMap;


public class DetailFragView extends Fragment {

	public static final String str_layout = "layoutId";

	protected View view=null;
	protected int layoutId=0;
	protected View title;
	protected View content;
	protected View headline;
	protected View date;
	protected View media;
	protected View subject;
	protected String item_title;
	protected String item_date;
	protected String item_path;
	protected String item_headline;
	protected String item_content;
	protected String item_type;
	protected String item_subject;


	public DetailFragView() {
        // Required empty public constructor
    }


	/**
	 * Static factory method that takes an int parameter,
	 * initializes the fragment's arguments, and returns the
	 * new fragment to the client.
	 */
	public static DetailFragView newInstance(HashMap<String,String> news) {
		Bundle args = new Bundle();
		for(String k:news.keySet()) {
			args.putCharArray(k,news.get(k).toCharArray());
		}

		String type = news.get(BsonProxy.HASH_FIELD_TYPE);
		DetailFragView f = null;
		if ( type != null) {
			if (type.equals(BsonProxy.TYPE_IMG_DIR)) {
				f = new DetailFragViewImage();
				args.putInt(str_layout,R.layout.fragment_detail_image_view);
			} else {
				f = new DetailFragView();
			}
		}
		if ( f!=null ) {
			f.setArguments(args);
		}
		return f;
	}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    if ( getArguments() != null) {
		    // Restore last state for checked position.
		    if(getArguments().getCharArray(BsonProxy.HASH_FIELD_PATH)!=null)
			    item_path =  String.valueOf(getArguments().getCharArray(BsonProxy.HASH_FIELD_PATH));
		    if(getArguments().getCharArray(BsonProxy.HASH_FIELD_TYPE)!=null)
			    item_type =  String.valueOf(getArguments().getCharArray(BsonProxy.HASH_FIELD_TYPE));
		    if(getArguments().getCharArray(BsonProxy.HASH_FIELD_DATE)!=null)
			    item_date =  String.valueOf(getArguments().getCharArray(BsonProxy.HASH_FIELD_DATE));
		    if(getArguments().getCharArray(BsonProxy.HASH_FIELD_TITLE)!=null)
			    item_title =  String.valueOf(getArguments().getCharArray(BsonProxy.HASH_FIELD_TITLE));
		    if(getArguments().getCharArray(BsonProxy.HASH_FIELD_HEADLINE)!=null)
			    item_headline =  String.valueOf(getArguments().getCharArray(BsonProxy.HASH_FIELD_HEADLINE));
		    if(getArguments().getCharArray(BsonProxy.HASH_FIELD_CONTENT)!=null)
			    item_content =  String.valueOf(getArguments().getCharArray(BsonProxy.HASH_FIELD_CONTENT));
		    if(getArguments().getCharArray(BsonProxy.HASH_FIELD_SUBJECT)!=null)
			    item_subject =  String.valueOf(getArguments().getCharArray(BsonProxy.HASH_FIELD_SUBJECT));
		    layoutId =  getArguments().getInt(str_layout);
	    }
        // Inflate the layout for this fragment
        view = inflater.inflate(layoutId, container, false);
	    media = view.findViewById(R.id.media);
	    title = view.findViewById(R.id.title);
	    headline = view.findViewById(R.id.headline);
	    subject = view.findViewById(R.id.subject);
	    content = view.findViewById(R.id.content);
	    date = view.findViewById(R.id.date);
	    return view;
    }

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


}
