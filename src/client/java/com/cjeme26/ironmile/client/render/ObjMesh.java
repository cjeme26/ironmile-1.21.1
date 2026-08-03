package com.cjeme26.ironmile.client.render;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small Wavefront OBJ reader for Iron Mile's low-poly vehicle meshes. */
final class ObjMesh {
	private final List<MeshVertex> triangles;

	private ObjMesh(List<MeshVertex> triangles) {
		this.triangles = triangles;
	}

	static ObjMesh load(String resourcePath) {
		try (InputStream stream = ObjMesh.class.getResourceAsStream(resourcePath)) {
			if (stream == null) {
				throw new IOException("Missing model resource " + resourcePath);
			}
			return parse(stream);
		} catch (IOException | RuntimeException exception) {
			IronMile.LOGGER.error("Could not load Iron Mile mesh {}", resourcePath, exception);
			return new ObjMesh(List.of());
		}
	}

	private static ObjMesh parse(InputStream stream) throws IOException {
		List<Vector3f> positions = new ArrayList<>();
		List<Vector2f> textureCoordinates = new ArrayList<>();
		List<Vector3f> normals = new ArrayList<>();
		List<MeshVertex> triangles = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				String[] parts = trimmed.split("\\s+");
				switch (parts[0]) {
					case "v" -> positions.add(new Vector3f(
							Float.parseFloat(parts[1]),
							Float.parseFloat(parts[2]),
							Float.parseFloat(parts[3])
					));
					case "vt" -> textureCoordinates.add(new Vector2f(
							Float.parseFloat(parts[1]),
							Float.parseFloat(parts[2])
					));
					case "vn" -> normals.add(new Vector3f(
							Float.parseFloat(parts[1]),
							Float.parseFloat(parts[2]),
							Float.parseFloat(parts[3])
					));
					case "f" -> triangulateFace(parts, positions, textureCoordinates, normals, triangles);
					default -> {
						// Object, group and material declarations are not needed at runtime.
					}
				}
			}
		}
		return new ObjMesh(List.copyOf(triangles));
	}

	private static void triangulateFace(
			String[] parts,
			List<Vector3f> positions,
			List<Vector2f> textureCoordinates,
			List<Vector3f> normals,
			List<MeshVertex> output
	) {
		List<MeshVertex> face = new ArrayList<>(parts.length - 1);
		for (int index = 1; index < parts.length; index++) {
			String[] indices = parts[index].split("/", -1);
			Vector3f position = positions.get(resolveIndex(indices[0], positions.size()));
			Vector2f uv = indices.length > 1 && !indices[1].isEmpty()
					? textureCoordinates.get(resolveIndex(indices[1], textureCoordinates.size()))
					: new Vector2f();
			Vector3f normal = indices.length > 2 && !indices[2].isEmpty()
					? normals.get(resolveIndex(indices[2], normals.size()))
					: null;
			face.add(new MeshVertex(new Vector3f(position), new Vector2f(uv), normal == null ? null : new Vector3f(normal)));
		}

		for (int index = 1; index < face.size() - 1; index++) {
			MeshVertex first = face.get(0);
			MeshVertex second = face.get(index);
			MeshVertex third = face.get(index + 1);
			Vector3f fallbackNormal = new Vector3f(second.position()).sub(first.position())
					.cross(new Vector3f(third.position()).sub(first.position()))
					.normalize();
			output.add(first.withFallbackNormal(fallbackNormal));
			output.add(second.withFallbackNormal(fallbackNormal));
			output.add(third.withFallbackNormal(fallbackNormal));
		}
	}

	private static int resolveIndex(String value, int size) {
		int parsed = Integer.parseInt(value);
		return parsed > 0 ? parsed - 1 : size + parsed;
	}

	void render(MatrixStack.Entry entry, VertexConsumer consumer, int light) {
		for (int index = 0; index < this.triangles.size(); index += 3) {
			this.emitVertex(entry, consumer, light, this.triangles.get(index));
			this.emitVertex(entry, consumer, light, this.triangles.get(index + 1));
			this.emitVertex(entry, consumer, light, this.triangles.get(index + 2));
			// Entity cutout layers use QUADS. Repeating the final point turns each
			// OBJ triangle into a valid degenerate quad without changing its shape.
			this.emitVertex(entry, consumer, light, this.triangles.get(index + 2));
		}
	}

	private void emitVertex(MatrixStack.Entry entry, VertexConsumer consumer, int light, MeshVertex vertex) {
		consumer.vertex(entry, vertex.position().x, vertex.position().y, vertex.position().z)
					.color(255, 255, 255, 255)
					.texture(vertex.uv().x, 1.0F - vertex.uv().y)
					.overlay(OverlayTexture.DEFAULT_UV)
					.light(light)
					.normal(entry, vertex.normal().x, vertex.normal().y, vertex.normal().z);
	}

	private record MeshVertex(Vector3f position, Vector2f uv, Vector3f normal) {
		private MeshVertex withFallbackNormal(Vector3f fallback) {
			return this.normal == null ? new MeshVertex(this.position, this.uv, new Vector3f(fallback)) : this;
		}
	}
}
