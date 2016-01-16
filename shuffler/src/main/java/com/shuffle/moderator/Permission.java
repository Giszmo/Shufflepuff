package com.shuffle.moderator;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 12/25/15.
 */
public class Permission implements Serializable{
    public enum ViewPermission {
        Public, // Anyone can view.
        Joiner, // Visable only to those who can join it.
        ByReference, // Visable only to those who know of the mix's existence.
        Private // Visible only to particular users.
    }

    public enum JoinPermission {
        Public, // Anyone can join.
        Private // Joinable only by specific users.
    }

    ViewPermission view;
    JoinPermission join;
}
