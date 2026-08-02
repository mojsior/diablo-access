#include "utils/screen_reader.hpp"

#include <string>
#include <string_view>

#ifdef _WIN32
#ifdef _MSC_VER
#pragma warning(push)
#pragma warning(disable : 4309)
#endif
#include <prism.h>
#ifdef _MSC_VER
#pragma warning(pop)
#endif
#elif defined(__ANDROID__)
#include "platform/android/android.hpp"
#else
#include <speech-dispatcher/libspeechd.h>
#endif

namespace devilution {

#ifdef _WIN32
namespace {
PrismContext *PrismContextHandle = nullptr;
PrismBackend *PrismScreenReaderBackend = nullptr;
} // namespace
#elif !defined(__ANDROID__)
SPDConnection *Speechd;
#endif

void InitializeScreenReader()
{
#ifdef _WIN32
	PrismConfig cfg = prism_config_init();
	PrismContextHandle = prism_init(&cfg);
	if (PrismContextHandle != nullptr)
		PrismScreenReaderBackend = prism_registry_acquire_best(PrismContextHandle);
#elif defined(__ANDROID__)
	devilution::accessibility::InitializeScreenReaderAndroid();
#else
	Speechd = spd_open("DevilutionX", "DevilutionX", NULL, SPD_MODE_SINGLE);
#endif
}

void ShutDownScreenReader()
{
#ifdef _WIN32
	prism_backend_free(PrismScreenReaderBackend);
	PrismScreenReaderBackend = nullptr;
	prism_shutdown(PrismContextHandle);
	PrismContextHandle = nullptr;
#elif defined(__ANDROID__)
	devilution::accessibility::ShutDownScreenReaderAndroid();
#else
	spd_close(Speechd);
#endif
}

void SpeakText(std::string_view text, bool force)
{
	static std::string SpokenText;

	if (!force && SpokenText == text)
		return;

	SpokenText = text;

#ifdef _WIN32
	if (PrismContextHandle == nullptr)
		return;
	if (PrismScreenReaderBackend == nullptr)
		PrismScreenReaderBackend = prism_registry_acquire_best(PrismContextHandle);
	if (PrismScreenReaderBackend != nullptr) {
		const PrismError error = prism_backend_output(PrismScreenReaderBackend, SpokenText.c_str(), true);
		if (error == PRISM_ERROR_BACKEND_NOT_AVAILABLE || error == PRISM_ERROR_LIBRARY_LOAD_FAILED) {
			prism_backend_free(PrismScreenReaderBackend);
			PrismScreenReaderBackend = nullptr;
		}
	}
#elif defined(__ANDROID__)
	devilution::accessibility::SpeakTextAndroid(SpokenText.c_str());
#else
	spd_say(Speechd, SPD_TEXT, SpokenText.c_str());
#endif
}

} // namespace devilution
