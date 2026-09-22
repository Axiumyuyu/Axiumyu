package xyz.axiumyu;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

class AxiumyuLoader implements PluginLoader {

    @Override
    public void classloader(final PluginClasspathBuilder builder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(
                new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build()
        );
        resolver.addDependency(
                new Dependency(new DefaultArtifact("org.jetbrains.kotlin:kotlin-stdlib:2.4.20"), null)
        );
        resolver.addDependency(new Dependency(
                new DefaultArtifact("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0"), null)
        );
        builder.addLibrary(resolver);

    }
}
