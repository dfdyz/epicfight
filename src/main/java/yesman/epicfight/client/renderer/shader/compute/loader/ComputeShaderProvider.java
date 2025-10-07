package yesman.epicfight.client.renderer.shader.compute.loader;

import java.nio.FloatBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup;
import yesman.epicfight.client.renderer.shader.compute.VanillaComputeShaderSetup;
import yesman.epicfight.client.renderer.shader.compute.backend.buffers.DynamicSSBO;
import yesman.epicfight.client.renderer.shader.compute.backend.buffers.IArrayBufferProxy;
import yesman.epicfight.client.renderer.shader.compute.backend.program.BarrierFlags;
import yesman.epicfight.client.renderer.shader.compute.backend.program.ComputeProgram;
import yesman.epicfight.client.renderer.shader.compute.iris.IrisComputeShaderSetup;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class ComputeShaderProvider {
	private static final int MIN_SHADER_STORAGE_BUFFER_BINDINGS = 14;
	
    public static ComputeProgram meshComputeVanilla;
    public static ComputeProgram meshComputeIris;
    
    private static boolean supportComputeShader = false;
    private static boolean irisLoaded = false;
    private static Function<SkinnedMesh, ComputeShaderSetup> computeShaderProvider = VanillaComputeShaderSetup::new;
    
    public static void initIris() {
    	irisLoaded = true;
    	computeShaderProvider = IrisComputeShaderSetup::new;
    }
    
    public static boolean supportComputeShader() {
    	return supportComputeShader;
    }
    
    public static boolean irisLoaded() {
        return irisLoaded;
    }
    
    public static void checkIfSupports() {
    	String glVersion = GL11C.glGetString(GL11C.GL_VERSION);
        int major = GL11C.glGetInteger(GL30C.GL_MAJOR_VERSION);
        int minor = GL11C.glGetInteger(GL30C.GL_MINOR_VERSION);
        int storageBuffers = GL11C.glGetInteger(GL43C.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS);
        
        supportComputeShader = ((major > 4) || (major == 4 && minor >= 3)) && storageBuffers > MIN_SHADER_STORAGE_BUFFER_BINDINGS;
        
        EpicFightMod.LOGGER.warn("[Computer Shader Acceleration] OpenGL Version: " + glVersion);
        EpicFightMod.LOGGER.warn("[Computer Shader Acceleration] Shader Storage Buffers: " + storageBuffers + " (should be greater than " + MIN_SHADER_STORAGE_BUFFER_BINDINGS + ")");
        EpicFightMod.LOGGER.warn("[Computer Shader Acceleration] Compute Shader: " + (supportComputeShader ? "Supported" : "Unsupported"));
    }
    
    public static void epicfight$registerComputeShaders(RegisterShadersEvent event) {
        if (!supportComputeShader) return;
        
        clear();
        
        try {
        	meshComputeVanilla = ComputeShaderLoader.LoadComputeShaderProgram(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "shaders/compute/vanilla_mesh_transformer.comp"), BarrierFlags.SHADER_STORAGE, BarrierFlags.VERTEX_ATTRIB_ARRAY);
        	if (irisLoaded) meshComputeIris = ComputeShaderLoader.LoadComputeShaderProgram(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "shaders/compute/iris_mesh_transformer.comp"), BarrierFlags.SHADER_STORAGE, BarrierFlags.VERTEX_ATTRIB_ARRAY);
        } catch (Exception e) {
        	EpicFightMod.LOGGER.warn("[Computer Shader Acceleration] Failed at compiling compute shaders due to: " + e);
        	supportComputeShader = false;
        }
    }
    
    public static void clear() {
        if (meshComputeVanilla != null) {
        	meshComputeVanilla.delete();
        }
        
        if (meshComputeIris != null) {
        	meshComputeIris.delete();
        }
    }
    
    public static ComputeShaderSetup getComputeShaderSetup(SkinnedMesh mesh) {
    	return computeShaderProvider.apply(mesh);
    }

    public static <T> IArrayBufferProxy createDynamicBuffer(T[] src, int srcSize, BiConsumer<T, FloatBuffer> uploader){
        return new DynamicSSBO<>(src, (short) srcSize, DynamicSSBO.DataMode.DYNAMIC, uploader);
    }
}
