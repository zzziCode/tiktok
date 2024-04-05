package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**@author zzzi
 * @date 2024/3/28 14:26
 * 每一个视频都对应这样一个实体，视频列表中的每一项都是这样一个对象
 */
@AllArgsConstructor
@NoArgsConstructor
public class VideoVO {
    private Long id;
    private UserVO author;
    private String play_url;
    private String cover_url;
    private Integer favorite_count;
    private Integer comment_count;
    private boolean  is_favorite;
    private String title;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserVO getAuthor() {
        return author;
    }

    public void setAuthor(UserVO author) {
        this.author = author;
    }

    public String getPlay_url() {
        return play_url;
    }

    public void setPlay_url(String play_url) {
        this.play_url = play_url;
    }

    public String getCover_url() {
        return cover_url;
    }

    public void setCover_url(String cover_url) {
        this.cover_url = cover_url;
    }

    public Integer getFavorite_count() {
        return favorite_count;
    }

    public void setFavorite_count(Integer favorite_count) {
        this.favorite_count = favorite_count;
    }

    public Integer getComment_count() {
        return comment_count;
    }

    public void setComment_count(Integer comment_count) {
        this.comment_count = comment_count;
    }

    public boolean getIs_favorite() {
        return is_favorite;
    }

    public void setIs_favorite(boolean is_favorite) {
        this.is_favorite = is_favorite;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**@author zzzi
     * @date 2024/4/2 18:08
     * 由于要进行比较，所以要重写equals和hashCode
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoVO videoVO = (VideoVO) o;
        return id.equals(videoVO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
