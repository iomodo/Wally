package com.wally.wally.datacontroller;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.wally.wally.objects.content.Content;
import com.wally.wally.datacontroller.content.FirebaseContent;
import com.wally.wally.objects.content.Visibility;
import com.wally.wally.datacontroller.firebase.FirebaseObject;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContentManagerTest {
    private static final String TEST_ID = "Test_Id";
    private ContentManager testSubject;

    private DatabaseReference rooms;
    @SuppressWarnings("FieldCanBeLocal")
    private StorageReference storage;
    private DatabaseReference contents;

    @Before
    public void init() {
        storage = mock(StorageReference.class);
        when(storage.child(anyString())).thenReturn(storage);

        rooms = mock(DatabaseReference.class);
        when(rooms.child(anyString())).thenReturn(rooms);

        contents = mock(DatabaseReference.class);
        when(contents.push()).thenReturn(contents);
        when(contents.getKey()).thenReturn(TEST_ID);
        when(contents.getParent()).thenReturn(contents);
        when(contents.child(anyString())).thenReturn(contents);

        testSubject = new ContentManager(rooms, contents, storage);
    }

    @After
    public void finish() {}

    @Test
    public void savePublicTest() {
        Content c = new Content();
        c.getVisibility().withSocialVisibility(Visibility.PUBLIC);
        saveTest(c, "Public");
    }

    @Test
    public void savePrivateTest() {
        Content c = new Content();
        c.getVisibility().withSocialVisibility(Visibility.PRIVATE);
        saveTest(c, c.getAuthorId());
    }

    @Test
    public void saveSharedTest() {
        Content c = new Content();
        c.getVisibility().withSocialVisibility(Visibility.SHARED);
        saveTest(c, "Shared");
    }

    @Test
    public void updatePublicTest() {
        Content c = new Content().withId(TEST_ID);
        c.getVisibility().withSocialVisibility(Visibility.PUBLIC);
        updateTest(c, "Public");
    }

    @Test
    public void updatePrivateTest() {
        Content c = new Content().withId(TEST_ID);
        c.getVisibility().withSocialVisibility(Visibility.PRIVATE);
        updateTest(c, c.getAuthorId());
    }

    @Test
    public void updateSharedTest() {
        Content c = new Content().withId(TEST_ID);
        c.getVisibility().withSocialVisibility(Visibility.SHARED);
        updateTest(c, "Shared");
    }

    private void updateTest(Content c, String node) {
        testSubject.save(c);
        verifyUpdate(node);
        validateSave(c);
    }


    private void saveTest(Content c, String node) {
        testSubject.save(c);
        verifySave(node);
        verifySaveInRoom(c);
        validateSave(c);
    }

    private void verifySave(String node) {
        verify(contents, never()).child(TEST_ID);
        InOrder inOrder = inOrder(contents);
        inOrder.verify(contents).child(node);
        inOrder.verify(contents).push();
    }

    private void validateSave(Content c) {
        assertEquals(TEST_ID, c.getId());
        ArgumentCaptor<FirebaseObject> captor =
                ArgumentCaptor.forClass(FirebaseObject.class);
        verify(contents).setValue(captor.capture());
        FirebaseContent object = (FirebaseContent) captor.getValue();
        assertEquals(c.getTitle(), object.getTitle());
        assertEquals(c.getNote(), object.getNote());
    }

    private void verifyUpdate(String node) {
        verify(contents, never()).push();
        InOrder inOrder = inOrder(contents);
        inOrder.verify(contents).child(node);
        inOrder.verify(contents).child(TEST_ID);
    }

    private void verifySaveInRoom(Content c) {
        InOrder inOrder = inOrder(rooms);
        inOrder.verify(rooms).child(c.getUuid());
        inOrder.verify(rooms).child("Contents");
        inOrder.verify(rooms).child(anyString());
        inOrder.verify(rooms).setValue(true);
    }

}