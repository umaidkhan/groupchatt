package com.hm.groupchat.Models;

import com.stfalcon.chatkit.commons.models.IMessage;

import java.util.Date;

public class Message implements IMessage {

    private String id;
    private String text;
    private Author author;
    private Date createdAt;

    public Message(String text, Author author, Date createdAt) {

        //todo: set message id
        //this.id =
        this.text = text;
        this.author = author;
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Author getUser() {
        return author;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }
}
