package com.nebula.launcher.catalog;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ModrinthModelTest {
    @Test public void parsesSearchProjectMetadataAndImage() throws Exception {
        JSONObject json = new JSONObject()
                .put("project_id", "abc123")
                .put("slug", "sample-mod")
                .put("title", "Sample Mod")
                .put("description", "A sample description")
                .put("project_type", "mod")
                .put("icon_url", "https://cdn.modrinth.com/data/abc123/icon.png")
                .put("author", "Nebula Team")
                .put("downloads", 12345);
        ModrinthClient.Project project = new ModrinthClient.Project(json);
        assertEquals("abc123", project.id);
        assertEquals("sample-mod", project.slug);
        assertEquals("Sample Mod", project.title);
        assertEquals("mod", project.projectType);
        assertEquals("https://cdn.modrinth.com/data/abc123/icon.png", project.iconUrl);
        assertEquals(12345L, project.downloads);
    }

    @Test public void parsesGalleryAndDependencyMetadata() throws Exception {
        JSONObject json = new JSONObject()
                .put("id", "abc123")
                .put("title", "Sample Mod")
                .put("project_type", "mod")
                .put("body", "Long description")
                .put("team", "team123")
                .put("gallery", new JSONArray()
                        .put(new JSONObject().put("url", "https://cdn.modrinth.com/data/abc123/shot.png").put("title", "In game"))
                        .put(new JSONObject().put("title", "Missing URL")))
                .put("dependencies", new JSONArray()
                        .put(new JSONObject().put("project_id", "dep123").put("dependency_type", "required"))
                        .put(new JSONObject().put("version_id", "version123").put("dependency_type", "optional")));
        ModrinthClient.ProjectDetails details = new ModrinthClient.ProjectDetails(json);
        assertEquals("Long description", details.body);
        assertEquals("team123", details.team);
        assertEquals(1, details.gallery.size());
        assertEquals("In game", details.gallery.get(0).title);
        assertEquals(2, details.dependencies.size());
        assertTrue(details.dependencies.get(0).isRequired());
        assertFalse(details.dependencies.get(1).isRequired());
        assertEquals("version123", details.dependencies.get(1).versionId);
    }

    @Test public void handlesProjectsWithoutOptionalMediaOrDependencies() throws Exception {
        JSONObject json = new JSONObject().put("id", "abc123").put("title", "No media");
        ModrinthClient.ProjectDetails details = new ModrinthClient.ProjectDetails(json);
        assertTrue(details.gallery.isEmpty());
        assertTrue(details.dependencies.isEmpty());
        assertTrue(details.authors.isEmpty());
    }
}
