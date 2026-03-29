export default function AboutPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-8">About Me</h1>

      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-8 space-y-6">
        <section>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">Profile</h2>
          <p className="text-gray-600 dark:text-gray-400 leading-relaxed">
            I am a developer passionate about building full-stack web applications.
            This portfolio blog showcases my projects, thoughts, and learning journey.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">Tech Stack</h2>
          <div className="flex flex-wrap gap-2">
            {['React', 'TypeScript', 'Spring Boot', 'PostgreSQL', 'Docker', 'Jenkins', 'Tailwind CSS'].map(tech => (
              <span
                key={tech}
                className="px-3 py-1 text-sm rounded-full bg-primary/10 text-primary dark:text-indigo-400 font-medium"
              >
                {tech}
              </span>
            ))}
          </div>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">This Project</h2>
          <p className="text-gray-600 dark:text-gray-400 leading-relaxed">
            A portfolio blog built with React + Spring Boot, containerized with Docker,
            and deployed via Jenkins CI/CD pipeline. Features include a markdown blog editor,
            project roadmap tracker, and responsive design with dark mode support.
          </p>
        </section>
      </div>
    </div>
  );
}
