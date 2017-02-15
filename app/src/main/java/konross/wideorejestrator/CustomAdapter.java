package konross.wideorejestrator;

/**
 * Created by Konrad on 2017-02-02.
 */

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class CustomAdapter extends BaseAdapter{//zmień nazwy!
    String [] result;
    String filesDirectory;
    Context context;
    int  imageId;
    private static LayoutInflater inflater=null;
    public CustomAdapter(VideosListActivity mainActivity, String[] prgmNameList, String directory) {
        // TODO Auto-generated constructor stub
        result=prgmNameList;
        filesDirectory=directory+"/";
        context=mainActivity;
        Toast.makeText(context, "AAAAAAAAAAA "+prgmNameList[0], Toast.LENGTH_SHORT).show();
        //imageId=;//new int[result.length];
        inflater = ( LayoutInflater )context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return result.length;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public class Holder
    {
        TextView tv;
        ImageView img, imageView;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        Holder holder=new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.row_view, null);
        holder.tv=(TextView) rowView.findViewById(R.id.textView1);
        holder.img=(ImageView) rowView.findViewById(R.id.imageView1);
        holder.tv.setText(result[position]);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            //holder.img.setImageBitmap(retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST));
        }
        catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
            }
        }
        if(holder.img==null){
            //holder.img.setImageDrawable();
            Toast.makeText(context, "IMG NUUUUULLLLL", Toast.LENGTH_SHORT).show();
        }

        //holder.img.setImageResource(imageId[position]);
        rowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Toast.makeText(context, "You Clicked "+result[position], Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(filesDirectory+result[position]));
                intent.setDataAndType(Uri.parse(filesDirectory+result[position]), "video/mp4");
                context.startActivity(intent);
            }
        });

        rowView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                final AlertDialog alertDialog = new AlertDialog.Builder(context).create();

                alertDialog.setTitle("Czy na pewno chcesz usunąć to nagranie: "+result[position]+"?");
                alertDialog.setButton(Dialog.BUTTON_POSITIVE,"TAK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(filesDirectory+result[position]);
                        //boolean deleted = file.delete();
                        file.delete();
                    }
                });
                alertDialog.setButton(Dialog.BUTTON_NEGATIVE,"NIE",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alertDialog.show();
                return false;
            }
        });
        return rowView;
    }

}
