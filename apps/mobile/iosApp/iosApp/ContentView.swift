import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        VStack(spacing: 20) {
            Text("🚛")
                .font(.system(size: 56))
            Text("FleetManager iOS")
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(.white)
            Text("En desarrollo — funcionalidad implementada en KMP shared module")
                .font(.caption)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
    }
}

#Preview { ContentView() }
