package com.example.livenews;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();

    SQLiteDatabase articlesDB;

    RecycleAdapter adapter;

    private View.OnClickListener onItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) view.getTag();
            int position = viewHolder.getAdapterPosition();
            Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
            intent.putExtra("content", content.get(position));

            startActivity(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, title VARCHAR, content VARCHAR)");

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://newsapi.org/v2/top-headlines?country=pt&pagesize=3&apiKey=USEYOUROWNAPIKEY");
        }catch (Exception e){
            e.printStackTrace();
        }


        RecyclerView recyclerView = findViewById(R.id.recycleView);
        adapter = new RecycleAdapter(titles);
        recyclerView.setAdapter(adapter);
        RecyclerView.LayoutManager listLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        RecyclerView.LayoutManager gridLayoutManager =  new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(gridLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter.setItemClickListener(onItemClickListener);

        updateListView();

    }

    public void updateListView() {
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {
            titles.clear();
            content.clear();

            do {

                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

            } while (c.moveToNext());

            adapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1){
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }

                articlesDB.execSQL("DELETE FROM articles");

                JSONObject jsonObject = new JSONObject(result);
                JSONArray jsonArticles = jsonObject.getJSONArray("articles");
                for (int i =0; i< jsonArticles.length();i++){
                    JSONObject jsonPart = jsonArticles.getJSONObject(i);
                    String title = jsonPart.getString("title");
                    String articleUrl = jsonPart.getString("url");
                    titles.add(title);

                    Log.i("Title and URL", title +"//"+articleUrl);

                    url = new URL(articleUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    data = inputStreamReader.read();
                    String articleContent = "";
                    while (data != -1){
                        char current = (char) data;
                        articleContent += current;
                        data = inputStreamReader.read();
                    }

                    Log.i("HTML", articleContent);

                    String sql = "INSERT INTO articles (title, content) VALUES (?, ?)";
                    SQLiteStatement statement = articlesDB.compileStatement(sql);
                    statement.bindString(1, title);
                    statement.bindString(2, articleContent);

                    statement.execute();
                }


                Log.i("URL COntent", result);
                return result;


            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }
}
