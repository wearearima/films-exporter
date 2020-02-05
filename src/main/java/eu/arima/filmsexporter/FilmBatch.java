package eu.arima.filmsexporter;

import com.google.gson.Gson;
import org.postgresql.util.PGobject;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.ExceptionClassifierSkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.*;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Component
public class FilmBatch {

    private static final String INSERT_FILM_SQL = "INSERT INTO film " +
            "(title, description, release_date, average_rating, genre_list, time, image)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    @Bean
    Job imdbToDatabaseJob(JobBuilderFactory jobBuilderFactory, Step imdbToDatabaseStep) {
       return jobBuilderFactory.get("imdbToDatabaseJob")
               .start(imdbToDatabaseStep)
               .build();
    }

    @Bean
    Step imdbToDatabaseStep(ItemReader<Film> filmReader, ItemWriter<Film> filmWriter,
                            StepBuilderFactory stepBuilderFactory) {

        return stepBuilderFactory.get("imdbToDatabaseStep")
                .<Film, Film>chunk(10)
                .reader(filmReader)
                .faultTolerant()
                .skipLimit(10)
                .skip(ScreenScrapingException.class)
                .writer(filmWriter)
                .build();
    }

    @Bean
    ItemReader<Film> filmReader(@Value("${imdb.link:https://www.imdb.com/chart/moviemeter/}") String imdbLink) {
        return new FilmReader(imdbLink);
    }

    @Bean
    ItemWriter<Film> filmWriter(DataSource dataSource) {
        JdbcBatchItemWriter<Film> filmWriter = new JdbcBatchItemWriter<>();
        filmWriter.setDataSource(dataSource);
        filmWriter.setSql(INSERT_FILM_SQL);
        filmWriter.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<Film>() {

            @Override
            public void setValues(Film item, PreparedStatement ps) throws SQLException {
                int i = 1;
                ps.setString(i++, item.getTitle());
                ps.setString(i++, item.getDescription());
                ps.setObject(i++, item.getReleaseDate());
                ps.setFloat(i++, item.getAverageRating());

                String genresJson = new Gson().toJson(item.getGenreList());
                PGobject pGobject = new PGobject();
                pGobject.setType("jsonb");
                pGobject.setValue(genresJson);

                ps.setObject(i++, pGobject);

                ps.setString(i++, item.getTime());
                ps.setBinaryStream(i++, new ByteArrayInputStream(item.getImage()));
            }

        });

        return filmWriter;
    }

    public static class FilmReader implements ItemReader<Film> {

        private final String filmListUrl;

        private final ImdbScreenScraper imdbScreenScraper = new ImdbScreenScraper();

        private List<String> movieLinkList;

        private int index;

        public FilmReader(String filmListUrl) {
            this.filmListUrl = filmListUrl;
        }

        @Override
        public Film read() {

            if (movieLinkList == null) {
                this.init();
            }

            if (movieLinkList.isEmpty() || movieLinkList.size() == index) {
                return null;
            }

            String link = nextMovieLink();

            Film film = this.imdbScreenScraper.scrapeFilmLink(link);

            return film;
        }

        private void init() {
            this.movieLinkList = this.imdbScreenScraper.scrapeMovieListLinks(this.filmListUrl);
        }

        private String nextMovieLink() {
            String link = this.movieLinkList.get(this.index);
            this.index++;
            return  link;
        }

    }

}
